package com.sistema.examenes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistema.examenes.modelo.Categoria;
import com.sistema.examenes.modelo.Examen;
import com.sistema.examenes.repositorios.CategoriaRepository;
import com.sistema.examenes.repositorios.ExamenRepository;
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
//@Transactional
public class ExamenControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExamenRepository examenRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    private Examen examen1;
    private Examen examen2;
    private Categoria categoria1;

    @BeforeEach
    void setUp() throws Exception {
        examenRepository.deleteAll();

        // Crear una categoría para asociarla a los exámenes
        categoria1 = new Categoria();
        categoria1.setTitulo("Categoría 1");
        categoria1.setDescripcion("Descripcion 1");
        categoria1 = categoriaRepository.save(categoria1);

        examen1 = new Examen();
        examen1.setTitulo("Examen 1");
        examen1.setDescripcion("Descripcion 1");
        examen1.setPuntosMaximos("100");
        examen1.setNumeroDePreguntas("10");
        examen1.setActivo(true);
        examen1.setCategoria(categoria1);
        examen1 = examenRepository.save(examen1);

        examen2 = new Examen();
        examen2.setTitulo("Examen 2");
        examen2.setDescripcion("Descripcion 2");
        examen2.setPuntosMaximos("120");
        examen2.setNumeroDePreguntas("12");
        examen2.setActivo(false);
        examen2.setCategoria(categoria1);
        examen2 = examenRepository.save(examen2);

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
    void testGuardarExamen() throws Exception {
        Examen nuevoExamen = new Examen();
        nuevoExamen.setTitulo("Examen Nuevo");
        nuevoExamen.setDescripcion("Descripcion Nueva");
        nuevoExamen.setPuntosMaximos("150");
        nuevoExamen.setNumeroDePreguntas("15");
        nuevoExamen.setActivo(true);
        nuevoExamen.setCategoria(categoria1); // Asignar la categoría creada

        String responseBody = mockMvc.perform(post("http://localhost:8080/examen/")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevoExamen)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.examenId").isNotEmpty())
                .andExpect(jsonPath("$.titulo").value("Examen Nuevo"))
                .andExpect(jsonPath("$.descripcion").value("Descripcion Nueva"))
                .andExpect(jsonPath("$.puntosMaximos").value("150"))
                .andExpect(jsonPath("$.numeroDePreguntas").value("15"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.categoria.categoriaId").value(categoria1.getCategoriaId())) // Verificar el categoriaId
                .andReturn().getResponse().getContentAsString();

        // Extraer el examen creado del response
        Examen examenCreado = objectMapper.readValue(responseBody, Examen.class);

        // Verificar que el examen se guardó en la base de datos
        Optional<Examen> examenGuardado = examenRepository.findById(examenCreado.getExamenId());
        assertThat(examenGuardado).isPresent();
        assertThat(examenGuardado.get().getTitulo()).isEqualTo("Examen Nuevo");
        assertThat(examenGuardado.get().getDescripcion()).isEqualTo("Descripcion Nueva");
        assertThat(examenGuardado.get().getPuntosMaximos()).isEqualTo("150");
        assertThat(examenGuardado.get().getNumeroDePreguntas()).isEqualTo("15");
        assertThat(examenGuardado.get().isActivo()).isEqualTo(true);
        assertThat(examenGuardado.get().getCategoria().getCategoriaId()).isEqualTo(categoria1.getCategoriaId()); // Verificar el categoriaId
    }

    @Test
    void testActualizarExamen() throws Exception {
        Examen examenActualizado = new Examen();
        examenActualizado.setExamenId(examen1.getExamenId());
        examenActualizado.setTitulo("Examen 1 Actualizado");
        examenActualizado.setDescripcion("Descripcion 1 Actualizada");
        examenActualizado.setPuntosMaximos("200");
        examenActualizado.setNumeroDePreguntas("20");
        examenActualizado.setActivo(false);
        examenActualizado.setCategoria(categoria1);

        mockMvc.perform(put("http://localhost:8080/examen/")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(examenActualizado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.examenId").value(examen1.getExamenId()))
                .andExpect(jsonPath("$.titulo").value("Examen 1 Actualizado"))
                .andExpect(jsonPath("$.descripcion").value("Descripcion 1 Actualizada"))
                .andExpect(jsonPath("$.puntosMaximos").value("200"))
                .andExpect(jsonPath("$.numeroDePreguntas").value("20"))
                .andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.categoria.categoriaId").value(categoria1.getCategoriaId()));

        Optional<Examen> examenGuardado = examenRepository.findById(examen1.getExamenId());
        assertThat(examenGuardado).isPresent();
        assertThat(examenGuardado.get().getTitulo()).isEqualTo("Examen 1 Actualizado");
        assertThat(examenGuardado.get().getDescripcion()).isEqualTo("Descripcion 1 Actualizada");
        assertThat(examenGuardado.get().getPuntosMaximos()).isEqualTo("200");
        assertThat(examenGuardado.get().getNumeroDePreguntas()).isEqualTo("20");
        assertThat(examenGuardado.get().isActivo()).isEqualTo(false);
        assertThat(examenGuardado.get().getCategoria().getCategoriaId()).isEqualTo(categoria1.getCategoriaId());
    }

    @Test
    void testListarExamenPorId() throws Exception {
        mockMvc.perform(get("http://localhost:8080/examen/" + examen1.getExamenId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.examenId").value(examen1.getExamenId()))
                .andExpect(jsonPath("$.titulo").value(examen1.getTitulo()))
                .andExpect(jsonPath("$.descripcion").value(examen1.getDescripcion()));
    }

    @Test
    void testListarExamenes() throws Exception {
        mockMvc.perform(get("http://localhost:8080/examen/")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].examenId").value(examen1.getExamenId()))
                .andExpect(jsonPath("$[0].titulo").value(examen1.getTitulo()))
                .andExpect(jsonPath("$[0].descripcion").value(examen1.getDescripcion()))
                .andExpect(jsonPath("$[0].puntosMaximos").value(examen1.getPuntosMaximos()))
                .andExpect(jsonPath("$[0].numeroDePreguntas").value(examen1.getNumeroDePreguntas()))
                .andExpect(jsonPath("$[0].activo").value(examen1.isActivo()))
                .andExpect(jsonPath("$[1].examenId").value(examen2.getExamenId()))
                .andExpect(jsonPath("$[1].titulo").value(examen2.getTitulo()))
                .andExpect(jsonPath("$[1].descripcion").value(examen2.getDescripcion()))
                .andExpect(jsonPath("$[1].puntosMaximos").value(examen2.getPuntosMaximos()))
                .andExpect(jsonPath("$[1].numeroDePreguntas").value(examen2.getNumeroDePreguntas()))
                .andExpect(jsonPath("$[1].activo").value(examen2.isActivo()));
    }


    @Test
    void testEliminarExamen() throws Exception {
        mockMvc.perform(delete("http://localhost:8080/examen/" + examen1.getExamenId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        Optional<Examen> examenEliminado = examenRepository.findById(examen1.getExamenId());
        assertThat(examenEliminado).isEmpty();
    }

}
