package cl.duocuc.blackout.roster;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestTokens.class)
class CrudApiTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired RosterRepository repository;
    static final String BASE = "/api/rosters";
    static final String BODY = "{\"name\":\"Blackout\",\"game\":\"Valorant\",\"label\":\"Equipo principal\",\"number\":\"01\",\"status\":\"Activo\",\"playerIds\":[1,2]}";
    @BeforeEach void clear() { repository.deleteAll(); }
    private long create() throws Exception {
        var result = mvc.perform(post(BASE).header("Authorization", "Bearer " + TestTokens.admin())
            .contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isCreated()).andExpect(header().exists("Location")).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
    @Test void crudCompletoConAdminYStaff() throws Exception {
        long id = create();
        mvc.perform(get(BASE).header("Authorization", "Bearer " + TestTokens.admin()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get(BASE + "/" + id).header("Authorization", "Bearer " + TestTokens.staff()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        String updated = BODY.replace("Blackout", "Academy").replace("[1,2]", "[2,1]");
        mvc.perform(put(BASE + "/" + id).header("Authorization", "Bearer " + TestTokens.staff())
            .contentType(MediaType.APPLICATION_JSON).content(updated))
            .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Academy"));
        mvc.perform(get(BASE + "/" + id).header("Authorization", "Bearer " + TestTokens.admin()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.playerIds[0]").value(2));
        mvc.perform(delete(BASE + "/" + id).header("Authorization", "Bearer " + TestTokens.admin()))
            .andExpect(status().isNoContent());
        mvc.perform(get(BASE + "/" + id).header("Authorization", "Bearer " + TestTokens.admin()))
            .andExpect(status().isNotFound());
    }
    @Test void fansNoPuedenCrearEditarNiEliminarAunqueTokenDigaAdmin() throws Exception {
        long id = create();
        String bearer = "Bearer " + TestTokens.fan();
        mvc.perform(post(BASE).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isForbidden());
        mvc.perform(put(BASE + "/" + id).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isForbidden());
        mvc.perform(delete(BASE + "/" + id).header("Authorization", bearer)).andExpect(status().isForbidden());
    }
    @Test void sinTokenYTokenInvalidoDevuelven401() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE).header("Authorization", "Bearer incorrecto")).andExpect(status().isUnauthorized());
    }
    @Test void entraSinRolAdministrativoNoPuedeEscribir() throws Exception {
        String token = TestTokens.signed(TestTokens.claims(TestTokens.ENTRA).claim("roles", java.util.List.of("Fan")));
        mvc.perform(post(BASE).header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
    }
    @Test void validaJsonYCambiosInexistentes() throws Exception {
        String bearer = "Bearer " + TestTokens.admin();
        mvc.perform(post(BASE).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post(BASE).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest());
        mvc.perform(put(BASE + "/99999").header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isNotFound());
        mvc.perform(delete(BASE + "/99999").header("Authorization", bearer)).andExpect(status().isNotFound());
    }
    @Test void swaggerYCorsDisponibles() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mvc.perform(options(BASE).header("Origin", "http://localhost:5173")
            .header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "authorization,content-type"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options(BASE).header("Origin", "https://untrusted.example")
            .header("Access-Control-Request-Method", "POST")).andExpect(status().isForbidden());
    }
    
    @Test void fansConsultanRoster() throws Exception {
        long id = create();
        mvc.perform(get(BASE).header("Authorization", "Bearer " + TestTokens.fan())).andExpect(status().isOk());
        mvc.perform(get(BASE + "/" + id).header("Authorization", "Bearer " + TestTokens.fan()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.playerIds[0]").value(1));
    }
    @Test void jugadoresPositivosYNoRepetidos() throws Exception {
        for (String ids : java.util.List.of("[1,1]", "[-1]", "[null]")) {
            mvc.perform(post(BASE).header("Authorization", "Bearer " + TestTokens.admin())
                .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("[1,2]", ids)))
                .andExpect(status().isBadRequest());
        }
    }
}
