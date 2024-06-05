package com.sistema.examenes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistema.examenes.modelo.Categoria;
import com.sistema.examenes.modelo.Examen;
import com.sistema.examenes.modelo.Pregunta;
import com.sistema.examenes.repositorios.CategoriaRepository;
import com.sistema.examenes.repositorios.ExamenRepository;
import com.sistema.examenes.repositorios.PreguntaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
public class PreguntaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PreguntaRepository preguntaRepository;

    @Autowired
    private ExamenRepository examenRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    private String token;

    @Autowired
    private ObjectMapper objectMapper;

    private Examen examen;
    private Pregunta pregunta;

    private Categoria categoria1;

    @BeforeEach
    void setUp() throws Exception {
        preguntaRepository.deleteAll();

        // Primero crea la categoría
        categoria1 = new Categoria();
        categoria1.setTitulo("Categoría 1");
        categoria1.setDescripcion("Descripcion 1");
        categoria1 = categoriaRepository.save(categoria1);

        // Luego crea el examen asociado a la categoría
        examen = new Examen();
        examen.setTitulo("Examen 1");
        examen.setDescripcion("Descripcion 1");
        examen.setPuntosMaximos("100");
        examen.setNumeroDePreguntas("10");
        examen.setCategoria(categoria1);
        examen.setActivo(true);
        examen = examenRepository.save(examen);

        // Luego crea la pregunta asociada al examen
        pregunta = new Pregunta();
        pregunta.setContenido("Contenido de la pregunta");
        pregunta.setImagen("URL de la imagen");
        pregunta.setOpcion1("Opción 1");
        pregunta.setOpcion2("Opción 2");
        pregunta.setOpcion3("Opción 3");
        pregunta.setOpcion4("Opción 4");
        pregunta.setRespuestaDada("Respuesta dada por el usuario");
        pregunta.setRespuesta("Respuesta correcta");
        pregunta.setExamen(examen);
        pregunta = preguntaRepository.save(pregunta);

        token = obtenerToken();
    }

    private String obtenerToken() throws Exception {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "admin");
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
    void testGuardarPregunta() throws Exception {
        Pregunta nuevaPregunta = new Pregunta();
        nuevaPregunta.setContenido("Contenido de la pregunta");
        nuevaPregunta.setImagen("URL de la imagen");
        nuevaPregunta.setOpcion1("Opción 1");
        nuevaPregunta.setOpcion2("Opción 2");
        nuevaPregunta.setOpcion3("Opción 3");
        nuevaPregunta.setOpcion4("Opción 4");
        nuevaPregunta.setRespuesta("Respuesta correcta");
        nuevaPregunta.setExamen(examen);

        String responseBody = mockMvc.perform(post("http://localhost:8080/pregunta/")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevaPregunta)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preguntaId").isNotEmpty())
                .andExpect(jsonPath("$.contenido").value("Contenido de la pregunta"))
                .andExpect(jsonPath("$.imagen").value("URL de la imagen"))
                .andExpect(jsonPath("$.opcion1").value("Opción 1"))
                .andExpect(jsonPath("$.opcion2").value("Opción 2"))
                .andExpect(jsonPath("$.opcion3").value("Opción 3"))
                .andExpect(jsonPath("$.opcion4").value("Opción 4"))
                .andExpect(jsonPath("$.respuesta").value("Respuesta correcta"))
                .andExpect(jsonPath("$.examen.examenId").value(examen.getExamenId()))
                .andReturn().getResponse().getContentAsString();

        // Extraer la pregunta creada del response
        Pregunta preguntaCreada = objectMapper.readValue(responseBody, Pregunta.class);

        // Verificar que la pregunta se guardó en la base de datos
        Optional<Pregunta> preguntaGuardada = preguntaRepository.findById(preguntaCreada.getPreguntaId());
        assertThat(preguntaGuardada).isPresent();
        assertThat(preguntaGuardada.get().getContenido()).isEqualTo("Contenido de la pregunta");
        assertThat(preguntaGuardada.get().getImagen()).isEqualTo("URL de la imagen");
        assertThat(preguntaGuardada.get().getOpcion1()).isEqualTo("Opción 1");
        assertThat(preguntaGuardada.get().getOpcion2()).isEqualTo("Opción 2");
        assertThat(preguntaGuardada.get().getOpcion3()).isEqualTo("Opción 3");
        assertThat(preguntaGuardada.get().getOpcion4()).isEqualTo("Opción 4");
        assertThat(preguntaGuardada.get().getRespuesta()).isEqualTo("Respuesta correcta");
        assertThat(preguntaGuardada.get().getExamen().getExamenId()).isEqualTo(examen.getExamenId());
    }

    @Test
    void testActualizarPregunta() throws Exception {
        Pregunta preguntaActualizada = new Pregunta();
        preguntaActualizada.setPreguntaId(pregunta.getPreguntaId());
        preguntaActualizada.setImagen("Nueva URL de la imagen");
        preguntaActualizada.setContenido("Nuevo contenido de la pregunta");
        preguntaActualizada.setOpcion1("Nueva Opción 1");
        preguntaActualizada.setOpcion2("Nueva Opción 2");
        preguntaActualizada.setOpcion3("Nueva Opción 3");
        preguntaActualizada.setOpcion4("Nueva Opción 4");
        preguntaActualizada.setRespuesta("Nueva respuesta correcta");
        preguntaActualizada.setExamen(examen);

        mockMvc.perform(put("http://localhost:8080/pregunta/")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(preguntaActualizada)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preguntaId").value(pregunta.getPreguntaId()))
                .andExpect(jsonPath("$.contenido").value("Nuevo contenido de la pregunta"))
                .andExpect(jsonPath("$.imagen").value("Nueva URL de la imagen"))
                .andExpect(jsonPath("$.opcion1").value("Nueva Opción 1"))
                .andExpect(jsonPath("$.opcion2").value("Nueva Opción 2"))
                .andExpect(jsonPath("$.opcion3").value("Nueva Opción 3"))
                .andExpect(jsonPath("$.opcion4").value("Nueva Opción 4"))
                .andExpect(jsonPath("$.respuesta").value("Nueva respuesta correcta"))
                .andExpect(jsonPath("$.examen.examenId").value(examen.getExamenId()));

        // Verificar que los cambios se reflejaron en la base de datos
        Optional<Pregunta> preguntaGuardada = preguntaRepository.findById(pregunta.getPreguntaId());
        assertThat(preguntaGuardada).isPresent();
        assertThat(preguntaGuardada.get().getContenido()).isEqualTo("Nuevo contenido de la pregunta");
        assertThat(preguntaGuardada.get().getImagen()).isEqualTo("Nueva URL de la imagen");
        assertThat(preguntaGuardada.get().getOpcion1()).isEqualTo("Nueva Opción 1");
        assertThat(preguntaGuardada.get().getOpcion2()).isEqualTo("Nueva Opción 2");
        assertThat(preguntaGuardada.get().getOpcion3()).isEqualTo("Nueva Opción 3");
        assertThat(preguntaGuardada.get().getOpcion4()).isEqualTo("Nueva Opción 4");
        assertThat(preguntaGuardada.get().getRespuesta()).isEqualTo("Nueva respuesta correcta");
        assertThat(preguntaGuardada.get().getExamen().getExamenId()).isEqualTo(examen.getExamenId());
    }

    @Test
    void testBuscarPreguntaPorId() throws Exception {
        mockMvc.perform(get("http://localhost:8080/pregunta/" + pregunta.getPreguntaId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preguntaId").value(pregunta.getPreguntaId()))
                .andExpect(jsonPath("$.contenido").value(pregunta.getContenido()))
                .andExpect(jsonPath("$.imagen").value(pregunta.getImagen()))
                .andExpect(jsonPath("$.opcion1").value(pregunta.getOpcion1()))
                .andExpect(jsonPath("$.opcion2").value(pregunta.getOpcion2()))
                .andExpect(jsonPath("$.opcion3").value(pregunta.getOpcion3()))
                .andExpect(jsonPath("$.opcion4").value(pregunta.getOpcion4()))
                .andExpect(jsonPath("$.respuesta").value(pregunta.getRespuesta()))
                .andExpect(jsonPath("$.examen.examenId").value(examen.getExamenId()));
    }

    @Test
    void testEliminarPregunta() throws Exception {
        mockMvc.perform(delete("http://localhost:8080/pregunta/" + pregunta.getPreguntaId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Verificar que la pregunta se eliminó de la base de datos
        Optional<Pregunta> preguntaEliminada = preguntaRepository.findById(pregunta.getPreguntaId());
        assertThat(preguntaEliminada).isEmpty();
    }

    @Test
    void testListarPreguntasDelExamen() throws Exception {
        mockMvc.perform(get("http://localhost:8080/pregunta/examen/" + examen.getExamenId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].preguntaId").value(pregunta.getPreguntaId()))
                .andExpect(jsonPath("$[0].contenido").value(pregunta.getContenido()))
                .andExpect(jsonPath("$[0].imagen").value(pregunta.getImagen()))
                .andExpect(jsonPath("$[0].opcion1").value(pregunta.getOpcion1()))
                .andExpect(jsonPath("$[0].opcion2").value(pregunta.getOpcion2()))
                .andExpect(jsonPath("$[0].opcion3").value(pregunta.getOpcion3()))
                .andExpect(jsonPath("$[0].opcion4").value(pregunta.getOpcion4()))
                .andExpect(jsonPath("$[0].respuesta").value(pregunta.getRespuesta()));
    }

    @Test
    void testListarPreguntaDelExamenComoAdministrador() throws Exception {
        mockMvc.perform(get("http://localhost:8080/pregunta/examen/todos/" + examen.getExamenId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].preguntaId").value(pregunta.getPreguntaId()))
                .andExpect(jsonPath("$[0].contenido").value(pregunta.getContenido()))
                .andExpect(jsonPath("$[0].imagen").value(pregunta.getImagen()))
                .andExpect(jsonPath("$[0].opcion1").value(pregunta.getOpcion1()))
                .andExpect(jsonPath("$[0].opcion2").value(pregunta.getOpcion2()))
                .andExpect(jsonPath("$[0].opcion3").value(pregunta.getOpcion3()))
                .andExpect(jsonPath("$[0].opcion4").value(pregunta.getOpcion4()))
                .andExpect(jsonPath("$[0].respuesta").value(pregunta.getRespuesta()));
    }


}
