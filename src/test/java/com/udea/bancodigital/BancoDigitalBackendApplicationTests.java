package com.udea.bancodigital;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Unica prueba que toca la base de datos real: levanta el contexto completo y
// por eso necesita el PostgreSQL de `docker compose up -d db`. Por eso lleva la
// etiqueta "integracion", que surefire excluye por defecto:
//     ./mvnw test                -> solo unitarias, sin Docker
//     ./mvnw -Pintegracion test  -> tambien esta
@SpringBootTest
@Tag("integracion")
class BancoDigitalBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
