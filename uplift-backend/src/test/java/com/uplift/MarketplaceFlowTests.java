package com.uplift;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uplift.repository.ProjectRepository;
import com.uplift.repository.ProposalRepository;
import com.uplift.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MarketplaceFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clearDatabase() {
        proposalRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void clientCanReceiveAndAcceptOneFreelancerProposal() throws Exception {
        String clientToken = register("client@example.test", "CLIENT");
        String freelancerToken = register("freelancer@example.test", "FREELANCER");

        String projectJson = mockMvc.perform(post("/api/projects")
                        .header("Authorization", bearer(clientToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Build a portfolio",
                                  "description": "Create a responsive portfolio website.",
                                  "category": "Web Development",
                                  "budgetMin": 5000,
                                  "budgetMax": 10000
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long projectId = objectMapper.readTree(projectJson).path("id").asLong();

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", bearer(clientToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProjects").value(1));

        String proposalJson = mockMvc.perform(post("/api/projects/{id}/proposals", projectId)
                        .header("Authorization", bearer(freelancerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 7500,
                                  "deliveryDays": 10,
                                  "coverLetter": "I can deliver this portfolio with responsive layouts."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long proposalId = objectMapper.readTree(proposalJson).path("id").asLong();

        mockMvc.perform(get("/api/proposals/inbox")
                        .header("Authorization", bearer(clientToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectTitle").value("Build a portfolio"))
                .andExpect(jsonPath("$[0].freelancerName").value("freelancer"));

        mockMvc.perform(patch("/api/proposals/{id}/status", proposalId)
                        .header("Authorization", bearer(clientToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ACCEPTED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", bearer(freelancerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inProgressProjects").value(1));

        mockMvc.perform(patch("/api/projects/{id}/status", projectId)
                        .header("Authorization", bearer(clientToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"COMPLETED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", bearer(freelancerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedProjects").value(1));
    }

    @Test
    void publicRegistrationCannotCreateAdministratorAccounts() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Unauthorized admin",
                                  "email": "admin@example.test",
                                  "password": "safe-password",
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void freelancerCannotSubmitMoreThanOneProposalToAProject() throws Exception {
        String clientToken = register("owner@example.test", "CLIENT");
        String freelancerToken = register("worker@example.test", "FREELANCER");
        String projectJson = mockMvc.perform(post("/api/projects")
                        .header("Authorization", bearer(clientToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Write API docs",
                                  "description": "Document a small service.",
                                  "category": "Writing",
                                  "budgetMin": 200,
                                  "budgetMax": 400
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode project = objectMapper.readTree(projectJson);
        String projectId = project.path("id").asText();
        String proposal = """
                {
                  "amount": 300,
                  "deliveryDays": 2,
                  "coverLetter": "I can write clear API documentation."
                }
                """;

        mockMvc.perform(post("/api/projects/{id}/proposals", projectId)
                        .header("Authorization", bearer(freelancerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(proposal))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/projects/{id}/proposals", projectId)
                        .header("Authorization", bearer(freelancerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(proposal))
                .andExpect(status().isConflict());
    }

    private String register(String email, String role) throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "email": "%s",
                                  "password": "safe-password",
                                  "role": "%s"
                                }
                                """.formatted(email.substring(0, email.indexOf('@')), email, role)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
