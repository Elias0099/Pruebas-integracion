package com.sistema.examenes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistema.examenes.modelo.Categoria;
import com.sistema.examenes.repositorios.CategoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class CategoriaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    private Categoria categoria1;
    private Categoria categoria2;

    @BeforeEach
    void setUp() throws Exception {
        categoriaRepository.deleteAll();

        categoria1 = new Categoria();
        categoria1.setTitulo("Categoria 1");
        categoria1.setDescripcion("Descripcion 1");
        categoria1 = categoriaRepository.save(categoria1);

        categoria2 = new Categoria();
        categoria2.setTitulo("Categoria 2");
        categoria2.setDescripcion("Descripcion 2");
        categoria2 = categoriaRepository.save(categoria2);

        token = obtenerToken();
    }

    private String obtenerToken() throws Exception {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "elias");
        loginRequest.put("password", "123");

        String response = mockMvc.perform(post("http://localhost:8080/generate-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Suponiendo que el token se encuentra en la propiedad "token" del JSON de respuesta
        Map<String, String> responseMap = objectMapper.readValue(response, HashMap.class);
        return responseMap.get("token");
    }

    @Test
    void testGuardarCategoria() throws Exception {
        Categoria nuevaCategoria = new Categoria();
        nuevaCategoria.setTitulo("Categoria Nueva");
        nuevaCategoria.setDescripcion("Descripcion Nueva");

        String responseBody = mockMvc.perform(post("http://localhost:8080/categoria/")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevaCategoria)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoriaId").isNotEmpty())
                .andExpect(jsonPath("$.titulo").value("Categoria Nueva"))
                .andExpect(jsonPath("$.descripcion").value("Descripcion Nueva"))
                .andReturn().getResponse().getContentAsString();

        // Extraer la categoría creada del response
        Categoria categoriaCreada = objectMapper.readValue(responseBody, Categoria.class);

        // Verificar que la categoría se guardó en la base de datos
        Optional<Categoria> categoriaGuardada = categoriaRepository.findById(categoriaCreada.getCategoriaId());
        assertThat(categoriaGuardada).isPresent();
        assertThat(categoriaGuardada.get().getTitulo()).isEqualTo("Categoria Nueva");
        assertThat(categoriaGuardada.get().getDescripcion()).isEqualTo("Descripcion Nueva");
    }

    @Test
    void testListarCategoriaPorId() throws Exception {
        mockMvc.perform(get("http://localhost:8080/categoria/" + categoria1.getCategoriaId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoriaId").value(categoria1.getCategoriaId()))
                .andExpect(jsonPath("$.titulo").value(categoria1.getTitulo()))
                .andExpect(jsonPath("$.descripcion").value(categoria1.getDescripcion()));
    }

    @Test
    void testListarCategorias() throws Exception {
        mockMvc.perform(get("http://localhost:8080/categoria/")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoriaId").value(categoria1.getCategoriaId()))
                .andExpect(jsonPath("$[0].titulo").value(categoria1.getTitulo()))
                .andExpect(jsonPath("$[0].descripcion").value(categoria1.getDescripcion()))
                .andExpect(jsonPath("$[1].categoriaId").value(categoria2.getCategoriaId()))
                .andExpect(jsonPath("$[1].titulo").value(categoria2.getTitulo()))
                .andExpect(jsonPath("$[1].descripcion").value(categoria2.getDescripcion()));
    }

    @Test
    void testActualizarCategoria() throws Exception {
        Categoria categoriaActualizada = new Categoria();
        categoriaActualizada.setCategoriaId(categoria1.getCategoriaId());
        categoriaActualizada.setTitulo("Categoria 1 Actualizada");
        categoriaActualizada.setDescripcion("Descripcion 1 Actualizada");

        mockMvc.perform(put("http://localhost:8080/categoria/")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoriaActualizada)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoriaId").value(categoria1.getCategoriaId()))
                .andExpect(jsonPath("$.titulo").value("Categoria 1 Actualizada"))
                .andExpect(jsonPath("$.descripcion").value("Descripcion 1 Actualizada"));

        // Verificar que la categoría se actualizó en la base de datos
        Optional<Categoria> categoriaGuardada = categoriaRepository.findById(categoria1.getCategoriaId());
        assertThat(categoriaGuardada).isPresent();
        assertThat(categoriaGuardada.get().getTitulo()).isEqualTo("Categoria 1 Actualizada");
        assertThat(categoriaGuardada.get().getDescripcion()).isEqualTo("Descripcion 1 Actualizada");
    }

    @Test
    void testEliminarCategoria() throws Exception {
        mockMvc.perform(delete("http://localhost:8080/categoria/" + categoria1.getCategoriaId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Verificar que la categoría se eliminó de la base de datos
        Optional<Categoria> categoriaEliminada = categoriaRepository.findById(categoria1.getCategoriaId());
        assertThat(categoriaEliminada).isEmpty();
    }
}
