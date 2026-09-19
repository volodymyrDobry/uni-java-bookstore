//package com.cengage.contenttaggingservice.unibookstore.controller;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.java.carparts.domain.dto.ProductCreateRequest;
//import com.java.carparts.domain.dto.ProductResponse;
//import com.java.carparts.exception.ProductNotFoundException;
//import com.java.carparts.service.ProductService;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
//import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.math.BigDecimal;
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@WebMvcTest(
//        controllers = ProductController.class,
//        excludeAutoConfiguration = {
//                OAuth2ClientAutoConfiguration.class,
//                OAuth2ResourceServerAutoConfiguration.class
//        }
//)
//@AutoConfigureMockMvc(addFilters = false)
//class ProductPublicEndpointsWebMvcTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @MockitoBean
//    private ProductService productService;
//
//    @Test
//    void getProducts_returnsJsonArray() throws Exception {
//        ProductResponse response = new ProductResponse();
//        response.setId(1);
//        response.setName("Brake Pad");
//        response.setPrice(new BigDecimal("45.99"));
//        when(productService.listAll()).thenReturn(List.of(response));
//
//        mockMvc.perform(get("/api/products"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[0].id").value(1))
//                .andExpect(jsonPath("$[0].name").value("Brake Pad"))
//                .andExpect(jsonPath("$[0].price").value(45.99));
//    }
//
//    @Test
//    void getProductById_notFound_returns404WithErrorBody() throws Exception {
//        when(productService.getById(404)).thenThrow(new ProductNotFoundException(404));
//
//        mockMvc.perform(get("/api/products/{id}", 404))
//                .andExpect(status().isNotFound())
//                .andExpect(jsonPath("$.message").value("Product not found: 404"));
//    }
//
//    @Test
//    void createProduct_missingRequiredFields_returns400_serviceNeverCalled() throws Exception {
//        ProductCreateRequest invalid = new ProductCreateRequest(
//                "", "", "", "", null, null, 0);
//
//        mockMvc.perform(post("/api/products")
//                        .contentType("application/json")
//                        .content(objectMapper.writeValueAsString(invalid)))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void createProduct_validRequest_returns201() throws Exception {
//        ProductCreateRequest valid = new ProductCreateRequest(
//                "SKU-1", "New Part", "ENGINE", "Bosch", null, new BigDecimal("10.00"), 5);
//        ProductResponse response = new ProductResponse();
//        response.setId(1);
//        when(productService.create(any(ProductCreateRequest.class))).thenReturn(response);
//
//        mockMvc.perform(post("/api/products")
//                        .contentType("application/json")
//                        .content(objectMapper.writeValueAsString(valid)))
//                .andExpect(status().isCreated());
//    }
//}
