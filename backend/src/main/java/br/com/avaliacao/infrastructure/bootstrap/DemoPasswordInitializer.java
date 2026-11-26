package br.com.avaliacao.infrastructure.bootstrap;

import br.com.avaliacao.application.usuario.ConfigurarSenhasDemoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.configurar-senhas-demo", havingValue = "true")
public class DemoPasswordInitializer implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DemoPasswordInitializer.class);
    private final ConfigurarSenhasDemoService service;
    private final String senha;

    public DemoPasswordInitializer(ConfigurarSenhasDemoService service, @Value("${DEMO_PASSWORD:}") String senha) {
        this.service = service;
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        int quantidade = service.configurar(senha);
        LOGGER.info("Senhas configuradas em {} contas de demonstração ainda sem senha", quantidade);
    }
}
