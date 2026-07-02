package com.fitfuel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitfuel.config.JwtService;
import com.fitfuel.subscription.Subscription;
import com.fitfuel.subscription.SubscriptionRepository;
import com.fitfuel.subscription.SubscriptionStatus;
import com.fitfuel.user.AppUser;
import com.fitfuel.user.Role;
import com.fitfuel.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void verifiesCustomerApiFlowEndToEnd() throws Exception {
        LocalDate subscriptionStart = LocalDate.now().plusDays(1);
        LocalDate subscriptionEnd = subscriptionStart.plusMonths(1);

        String signupResponse = mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Aniket",
                                  "email": "aniket@example.com",
                                  "phone": "9999999999",
                                  "password": "password123",
                                  "age": 24,
                                  "weight": 72.5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("aniket@example.com"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String customerToken = objectMapper.readTree(signupResponse).get("token").asText();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "aniket@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.name").value("Aniket"));

        mockMvc.perform(get("/users/me")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("9999999999"));

        mockMvc.perform(put("/users/me")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Aniket Das",
                                  "weight": 73
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Aniket Das"))
                .andExpect(jsonPath("$.weight").value(73.0));

        String adminToken = createAdminToken();
        long foodItemId = createFoodItem(adminToken);

        mockMvc.perform(put("/menu/" + foodItemId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Paneer Protein Bowl Plus",
                                  "description": "Paneer, rice, veggies, salad, and curd",
                                  "category": "BOWL",
                                  "menuType": "REGULAR_MENU",
                                  "protein": 48,
                                  "calories": 680,
                                  "price": 219,
                                  "active": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Paneer Protein Bowl Plus"))
                .andExpect(jsonPath("$.price").value(219.0));

        mockMvc.perform(get("/menu?type=REGULAR_MENU"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].menuType").value("REGULAR_MENU"));

        mockMvc.perform(get("/menu/" + foodItemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Paneer Protein Bowl Plus"));

        mockMvc.perform(post("/cart/items")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "foodItemId": %d,
                                  "quantity": 2
                                }
                                """.formatted(foodItemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.totalAmount").value(438.0));

        mockMvc.perform(get("/cart")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].foodItemId").value(foodItemId));

        String checkoutResponse = mockMvc.perform(post("/orders/checkout")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long orderId = objectMapper.readTree(checkoutResponse).get("id").asLong();

        mockMvc.perform(get("/orders")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(orderId));

        mockMvc.perform(post("/payments")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": %d,
                                  "amount": 438,
                                  "provider": "RAZORPAY"
                                }
                                """.formatted(orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.gatewayReference").isNotEmpty());

        mockMvc.perform(get("/payments")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(438.0));

        String subscriptionResponse = mockMvc.perform(post("/subscriptions")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planType": "WEIGHT_LOSS",
                                  "proteinTier": "G150",
                                  "mealType": "FULL_DAY",
                                  "startDate": "%s",
                                  "endDate": "%s"
                                }
                                """.formatted(subscriptionStart, subscriptionEnd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planType").value("WEIGHT_LOSS"))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long subscriptionId = objectMapper.readTree(subscriptionResponse).get("id").asLong();

        mockMvc.perform(get("/subscriptions")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].proteinTier").value("G150"));

        mockMvc.perform(get("/subscriptions/active")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isNotFound());

        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElseThrow();
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(subscription);

        mockMvc.perform(get("/subscriptions/active")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(subscriptionId))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(post("/feedbacks")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": %d,
                                  "rating": 5,
                                  "comment": "Great meal"
                                }
                                """.formatted(orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Great meal"));

        mockMvc.perform(get("/feedbacks")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(orderId));
    }

    private long createFoodItem(String adminToken) throws Exception {
        String response = mockMvc.perform(post("/menu")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Paneer Protein Bowl",
                                  "description": "Paneer, rice, veggies, and salad",
                                  "category": "BOWL",
                                  "menuType": "REGULAR_MENU",
                                  "protein": 42,
                                  "calories": 620,
                                  "price": 199,
                                  "active": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Paneer Protein Bowl"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("id").asLong();
    }

    private String createAdminToken() {
        AppUser admin = new AppUser();
        admin.setName("Admin");
        admin.setEmail("admin@example.com");
        admin.setPhone("8888888888");
        admin.setPasswordHash(passwordEncoder.encode("password123"));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        assertThat(userRepository.findByEmail("admin@example.com")).isPresent();
        return jwtService.createToken("admin@example.com");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
