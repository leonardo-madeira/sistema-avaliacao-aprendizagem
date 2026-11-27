package br.com.avaliacao.infrastructure.security;

import br.com.avaliacao.domain.usuario.UsuarioRepository;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Locale;

@Configuration
public class SecurityConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(UsuarioRepository usuarios) {
        return email -> usuarios.buscarPorEmail(email.strip().toLowerCase(Locale.ROOT))
                .map(UsuarioPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, UserDetailsService usuarios,
                                           PasswordEncoder encoder) throws Exception {
        var provider = new DaoAuthenticationProvider(usuarios);
        provider.setPasswordEncoder(encoder);
        http.authenticationProvider(provider)
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/", "/index.html", "/app.js", "/avaliacoes.js", "/styles.css", "/favicon.svg",
                                "/realizacao.js", "/correcao.js",
                                "/relatorios.js",
                                "/api/auth/csrf", "/api/auth/login").permitAll()
                        .requestMatchers("/api/questoes", "/api/questoes/**").hasRole("PROFESSOR")
                        .requestMatchers("/api/avaliacoes", "/api/avaliacoes/**", "/api/alunos").hasRole("PROFESSOR")
                        .requestMatchers("/api/correcoes", "/api/correcoes/**").hasRole("PROFESSOR")
                        .requestMatchers("/api/relatorios", "/api/relatorios/**").hasRole("PROFESSOR")
                        .requestMatchers("/api/minhas-avaliacoes", "/api/minhas-avaliacoes/**").hasRole("ALUNO")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .requestCache(cache -> cache.disable())
                .formLogin(login -> login.loginPage("/").loginProcessingUrl("/api/auth/login")
                        .usernameParameter("email").passwordParameter("senha")
                        .successHandler((request, response, authentication) -> response.setStatus(204))
                        .failureHandler((request, response, exception) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"detail\":\"E-mail ou senha inválidos.\"}");
                        }))
                .logout(logout -> logout.logoutUrl("/api/auth/logout")
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"detail\":\"Entre na sua conta para continuar.\"}");
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"detail\":\"Acesso não permitido. Atualize a página e confira sua conta.\"}");
                        }));
        return http.build();
    }
}
