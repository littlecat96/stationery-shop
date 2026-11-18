package com.campus.stationery;

import com.campus.stationery.entity.CustomerOrder;
import com.campus.stationery.entity.Product;
import com.campus.stationery.repository.CustomerOrderRepository;
import com.campus.stationery.repository.ProductRepository;
import com.campus.stationery.service.CartService;
import com.campus.stationery.service.OrderService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.inOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:stationery_test;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
class StationeryShopApplicationTests {

    private static final Pattern CSRF_INPUT = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter securityFilter;

    @MockitoSpyBean
    private ProductRepository products;

    @Autowired
    private CustomerOrderRepository orders;

    @Autowired
    private CartService cart;

    @Autowired
    private OrderService orderService;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
    }

    @Test
    void productDeletionRequiresPostCsrfAndAdminRole() throws Exception {
        Product product = product("Delete candidate", 3);
        String path = "/admin/products/delete/" + product.getId();
        MockHttpSession admin = session(101L, "ADMIN");
        MockHttpSession buyer = session(102L, "USER");
        String adminToken = csrfFrom(mvc.perform(get("/admin/products").session(admin))
                .andExpect(status().isOk()).andReturn());
        String buyerToken = csrfFrom(mvc.perform(get("/shop").session(buyer))
                .andExpect(status().isOk()).andReturn());

        mvc.perform(get(path).session(admin)).andExpect(status().isMethodNotAllowed());
        mvc.perform(post(path).session(admin)).andExpect(status().isForbidden());
        mvc.perform(post(path).session(admin).param("_csrf", "invalid")).andExpect(status().isForbidden());
        mvc.perform(post(path).session(buyer).param("_csrf", buyerToken))
                .andExpect(redirectedUrl("/login"));
        assertThat(products.existsById(product.getId())).isTrue();

        mvc.perform(post(path).session(admin).param("_csrf", adminToken))
                .andExpect(redirectedUrl("/admin/products"));
        assertThat(products.existsById(product.getId())).isFalse();
    }

    @Test
    void buyerCanRegisterSignInAndFindOrderAfterSigningInAgain() throws Exception {
        Product product = product("Checkout notebook", 1);
        MockHttpSession registration = new MockHttpSession();
        String registerToken = csrfFrom(mvc.perform(get("/register").session(registration))
                .andExpect(status().isOk()).andReturn());
        mvc.perform(post("/register").session(registration)
                        .param("username", "checkout-buyer").param("password", "test-password-123"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/register").session(registration).param("_csrf", registerToken)
                        .param("username", "checkout-buyer").param("password", "test-password-123"))
                .andExpect(redirectedUrl("/login"));
        MockHttpSession buyer = login();
        String shopToken = csrfFrom(mvc.perform(get("/shop").session(buyer).param("keyword", "notebook"))
                .andExpect(content().string(containsString("Checkout notebook"))).andReturn());
        mvc.perform(post("/cart/add/{id}", product.getId()).session(buyer).param("_csrf", shopToken))
                .andExpect(redirectedUrl("/cart"));
        String cartToken = csrfFrom(mvc.perform(get("/cart").session(buyer))
                .andExpect(status().isOk()).andReturn());
        mvc.perform(post("/order/create").session(buyer)).andExpect(status().isForbidden());
        mvc.perform(post("/order/create").session(buyer).param("_csrf", cartToken))
                .andExpect(status().is3xxRedirection());
        Long buyerId = (Long) buyer.getAttribute("userId");
        List<CustomerOrder> buyerOrders = orders.findByUserIdOrderByCreatedAtDescIdDesc(buyerId);
        assertThat(buyerOrders).hasSize(1);
        Long orderId = buyerOrders.getFirst().getId();

        mvc.perform(get("/logout").session(buyer)).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/logout").session(buyer)).andExpect(status().isForbidden());
        mvc.perform(post("/logout").session(buyer).param("_csrf", shopToken))
                .andExpect(redirectedUrl("/login"));
        assertThat(buyer.isInvalid()).isTrue();

        MockHttpSession returningBuyer = login();
        mvc.perform(get("/order").session(returningBuyer))
                .andExpect(status().isOk()).andExpect(content().string(containsString("/order/" + orderId)));
        mvc.perform(get("/order/{id}", orderId).session(returningBuyer))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Checkout notebook")));
        mvc.perform(get("/order/{id}", orderId).session(session(999L, "USER")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/order/success/{id}", orderId).session(session(999L, "USER")))
                .andExpect(status().isNotFound());

        MockHttpSession admin = session(401L, "ADMIN");
        String adminToken = csrfFrom(mvc.perform(get("/admin/orders/{id}", orderId).session(admin))
                .andExpect(status().isOk()).andReturn());
        mvc.perform(post("/admin/orders/{id}/status", orderId).session(admin).param("status", "已完成"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/orders/{id}/status", orderId).session(admin)
                        .param("_csrf", adminToken).param("status", "已完成"))
                .andExpect(redirectedUrl("/admin/orders/" + orderId));
        assertThat(orders.findById(orderId).orElseThrow().getStatus()).isEqualTo("已完成");
    }

    @Test
    void twoBuyersCompetingForLastUnitCreateOneOrder() throws Exception {
        Product product = product("Last unit", 1);
        cart.addToCart("first-buyer", product.getId());
        cart.addToCart("second-buyer", product.getId());

        List<Boolean> outcomes = runTogether(
                () -> submitWithLimitedStock("first-buyer", 201L),
                () -> submitWithLimitedStock("second-buyer", 202L));

        assertThat(outcomes).containsExactlyInAnyOrder(true, false);
        assertThat(stock(product)).isZero();
        assertThat(orders.findByUserIdOrderByCreatedAtDescIdDesc(201L).size()
                + orders.findByUserIdOrderByCreatedAtDescIdDesc(202L).size()).isEqualTo(1);
    }

    @Test
    void failedCheckoutRollsBackStockAndRetainsCart() {
        Product first = product("Rollback item A", 3);
        Product second = product("Rollback item B", 3);
        String sessionId = "rollback-buyer";
        cart.addToCart(sessionId, first.getId());
        cart.addToCart(sessionId, second.getId());
        var items = cart.getCart(sessionId);
        Product available = items.getFirst().getProduct();
        Long soldOutId = items.getLast().getProduct().getId();
        Product soldOut = products.findById(soldOutId).orElseThrow();
        soldOut.setStock(0);
        products.saveAndFlush(soldOut);

        assertThatThrownBy(() -> orderService.createOrder(sessionId, 203L))
                .hasMessageContaining("库存不足");

        var deductions = inOrder(products);
        deductions.verify(products).deductStock(available.getId(), 1);
        deductions.verify(products).deductStock(soldOutId, 1);
        assertThat(stock(available)).isEqualTo(3);
        assertThat(stock(soldOut)).isZero();
        assertThat(cart.getCart(sessionId)).hasSize(2);
        assertThat(orders.findByUserIdOrderByCreatedAtDescIdDesc(203L)).isEmpty();
    }

    @Test
    void concurrentCancellationRestoresStockOnlyOnce() throws Exception {
        Product product = product("Cancel item", 3);
        cart.addToCart("cancel-buyer", product.getId());
        Long orderId = orderService.createOrder("cancel-buyer", 204L).getId();
        assertThat(stock(product)).isEqualTo(2);

        Callable<Void> cancel = () -> {
            orderService.updateStatus(orderId, "已取消");
            return null;
        };
        runTogether(cancel, cancel);

        assertThat(stock(product)).isEqualTo(3);
        assertThat(orders.findById(orderId).orElseThrow().getStatus()).isEqualTo("已取消");
    }

    private MockHttpSession login() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String token = csrfFrom(mvc.perform(get("/login").session(session))
                .andExpect(status().isOk()).andReturn());
        mvc.perform(post("/login").session(session).param("_csrf", token)
                        .param("username", "checkout-buyer").param("password", "test-password-123"))
                .andExpect(redirectedUrl("/shop"));
        return session;
    }

    private static MockHttpSession session(Long userId, String role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", userId);
        session.setAttribute("role", role);
        return session;
    }

    private static String csrfFrom(MvcResult result) throws Exception {
        var matcher = CSRF_INPUT.matcher(result.getResponse().getContentAsString());
        assertThat(matcher.find()).as("CSRF input in rendered form").isTrue();
        return matcher.group(1);
    }

    private Product product(String name, int stock) {
        Product product = new Product();
        product.setName(name);
        product.setCategory("测试文具");
        product.setPrice(new BigDecimal("2.50"));
        product.setStock(stock);
        product.setStatus(1);
        return products.saveAndFlush(product);
    }

    private int stock(Product product) {
        return products.findById(product.getId()).orElseThrow().getStock();
    }

    private boolean submitWithLimitedStock(String sessionId, Long userId) {
        try {
            orderService.createOrder(sessionId, userId);
            return true;
        } catch (RuntimeException ex) {
            assertThat(ex).hasMessageContaining("库存不足");
            return false;
        }
    }

    private static <T> List<T> runTogether(Callable<T> first, Callable<T> second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var firstResult = executor.submit(() -> {
                ready.countDown();
                assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                return first.call();
            });
            var secondResult = executor.submit(() -> {
                ready.countDown();
                assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                return second.call();
            });
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return java.util.Arrays.asList(firstResult.get(10, TimeUnit.SECONDS),
                    secondResult.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }
}
