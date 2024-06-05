package com.sistema.examenes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistema.examenes.modelo.Rol;
import com.sistema.examenes.modelo.Usuario;
import com.sistema.examenes.modelo.UsuarioRol;
import com.sistema.examenes.repositorios.RolRepository;
import com.sistema.examenes.repositorios.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class UsuarioControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    private Usuario usuario;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        usuarioRepository.deleteAll();
        rolRepository.deleteAll();

        // Crear un rol
        Rol rol = new Rol();
        rol.setRolId(1L);
        rol.setRolNombre("ADMIN");
        rolRepository.save(rol);

        // Crear un usuario
        usuario = new Usuario();
        usuario.setUsername("admin");
        usuario.setPassword("123");
        usuario.setNombre("Admin");
        usuario.setApellido("User");
        usuario.setEmail("admin@user.com");
        usuario.setTelefono("1234567890");
        usuario.setPerfil("default.png");

        UsuarioRol usuarioRol = new UsuarioRol();
        usuarioRol.setUsuario(usuario);
        usuarioRol.setRol(rol);

        Set<UsuarioRol> usuarioRoles = new HashSet<>();
        usuarioRoles.add(usuarioRol);

        usuario.setPassword(this.bCryptPasswordEncoder.encode(usuario.getPassword()));
        usuario = usuarioRepository.save(usuario);

        // Obtener el token de autenticación
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
    public void testGuardarUsuario() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setUsername("raul");
        usuario.setPassword("123");
        usuario.setNombre("Raul");
        usuario.setApellido("Valle");
        usuario.setEmail("valle@gmail.com");
        usuario.setTelefono("932231234");

        // Convertimos el objeto usuario a JSON
        String usuarioJson = objectMapper.writeValueAsString(usuario);

        // Realizamos la petición POST
        ResultActions resultActions = mockMvc.perform(post("/usuarios/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usuarioJson))
                .andExpect(status().isOk());

        // Verificamos que el usuario ha sido creado correctamente
        resultActions.andExpect(jsonPath("$.username").value("raul"))
                .andExpect(jsonPath("$.nombre").value("Raul"))
                .andExpect(jsonPath("$.apellido").value("Valle"))
                .andExpect(jsonPath("$.email").value("valle@gmail.com"))
                .andExpect(jsonPath("$.telefono").value("932231234"))
                .andExpect(jsonPath("$.perfil").value("default.png"))
                .andExpect(jsonPath("$.authorities[0].authority").value("NORMAL"));
    }


    @Test
    void testObtenerUsuario() throws Exception {
        mockMvc.perform(get("http://localhost:8080/usuarios/" + usuario.getUsername())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(usuario.getUsername()))
                .andExpect(jsonPath("$.email").value(usuario.getEmail()))
                .andExpect(jsonPath("$.perfil").value("default.png"));
    }

    @Test
    void testEliminarUsuario() throws Exception {
        mockMvc.perform(delete("http://localhost:8080/usuarios/" + usuario.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        Optional<Usuario> usuarioEliminado = usuarioRepository.findById(usuario.getId());
        assertThat(usuarioEliminado).isEmpty();
    }
}
