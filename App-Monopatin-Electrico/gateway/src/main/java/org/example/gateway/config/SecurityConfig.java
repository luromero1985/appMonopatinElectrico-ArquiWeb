package org.example.gateway.config;

import org.example.gateway.security.AuthotityConstant;
import org.example.gateway.security.jwt.JwtFilter;
import org.example.gateway.security.jwt.TokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final TokenProvider tokenProvider;

    public SecurityConfig( TokenProvider tokenProvider ) {
        this.tokenProvider = tokenProvider;
    }
/*
* El password debe encriptarse antes de almacenarlo en la base de datos para garantizar
*  la seguridad. Spring Security ofrece el PasswordEncoder*/
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        HttpSecurity httpSecurity = http
                .csrf(AbstractHttpConfigurer::disable) //lo desactivamos porque es ApiRest
                /*
                 * Lo siguiente establece que la aplicación no guardará información de sesión,
                 * es adecuado para una arquitectura de microservicios basada en tokens (como JWT).
                 *  El servidor no mantiene estado sobre las solicitudes, es decir, que cada solicitud necesita
                 * ser autenticada de manera independiente mediante el token JWT.*/
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(authz -> authz
                        /*Rutas públicas. Esto permite que las rutas de autenticación y registro de usuario sean accesibles
                        * sin autenticación previa, lo cual es necesario para que los usuarios puedan registrarse y obtener su token JWT.
                        */
                          .requestMatchers(HttpMethod.POST, "/authenticate").permitAll()
                          .requestMatchers(HttpMethod.POST, "/auth/usuario").permitAll()


                        /* Microservicio Administrador. Las rutas de administración están restringidas exclusivamente
                        a los administradores.
                        * */
                        .requestMatchers("/administrador/**").hasAuthority(AuthotityConstant._ADMIN)

                        /* Microservicio Monopatín. Los usuarios pueden consultar los monopatines, pero solo los usuarios
                        con el rol de MANTENIMIENTO puedan agregar, quitar o poner en mantenimiento los monopatines.
                        * */

                        .requestMatchers(HttpMethod.GET, "/monopatin/monopatines-cercanos/latitud/{latitud}/longitud/{longitud}/rango/{rango}").hasAnyAuthority(AuthotityConstant._ADMIN,AuthotityConstant._USUARIO)
                        .requestMatchers(HttpMethod.GET, "/monopatin").hasAnyAuthority(AuthotityConstant._ADMIN)
                        .requestMatchers(HttpMethod.GET, "/monopatin/{id}").hasAnyAuthority(AuthotityConstant._ADMIN)
                        .requestMatchers(HttpMethod.GET, "/monopatin/reportes/kilometraje").hasAnyAuthority(AuthotityConstant._ADMIN)
                        .requestMatchers(HttpMethod.GET, "/monopatin/reportes/tiempo-con-pausas").hasAnyAuthority(AuthotityConstant._ADMIN)
                        .requestMatchers(HttpMethod.GET, "/monopatin/reportes/tiempo-sin-pausas").hasAnyAuthority(AuthotityConstant._ADMIN)
                        .requestMatchers(HttpMethod.GET, "/monopatin/estado").hasAnyAuthority(AuthotityConstant._ADMIN)
                        .requestMatchers(HttpMethod.POST, "/monopatin/**").hasAuthority(AuthotityConstant._ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/monopatin/**").hasAuthority(AuthotityConstant._ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/monopatin/**").hasAuthority(AuthotityConstant._ADMIN)

                        /* Microservicio Cuenta
                        /* Las operaciones relacionadas con cuentas estan protegidas por autenticación.*/
                        .requestMatchers(HttpMethod.GET, "/cuenta/**").hasAuthority(AuthotityConstant._ADMIN)
                        .requestMatchers(HttpMethod.POST, "/cuenta/**").hasAuthority(AuthotityConstant._ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/cuenta/**").hasAuthority(AuthotityConstant._ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/cuenta/**").hasAuthority(AuthotityConstant._ADMIN)

                        /* Microservicio Facturación.
                        *  facturacion solo es accesible para los usuarios con el rol de ADMIN
                        * */
                        .requestMatchers("/facturacion/**").hasAuthority(AuthotityConstant._ADMIN)

                        /* Microservicio Estación/*/
                        .requestMatchers("/estacion/**").hasAnyAuthority(AuthotityConstant._ADMIN)


                       // Microservicio Viaje
                        .requestMatchers("/viaje/**").hasAnyAuthority(AuthotityConstant._ADMIN)

                        // Cualquier otra solicitud debe estar autenticada

                                // Microservicio usuario
                        .requestMatchers("/usuarios/**").hasAnyAuthority(AuthotityConstant._ADMIN, AuthotityConstant._USUARIO)

                     .anyRequest().authenticated()

                )
                .httpBasic(Customizer.withDefaults())
                .addFilterBefore(new JwtFilter(this.tokenProvider), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

}
