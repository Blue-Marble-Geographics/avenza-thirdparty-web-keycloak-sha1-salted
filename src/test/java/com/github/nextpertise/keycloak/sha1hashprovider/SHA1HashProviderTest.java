package com.github.nextpertise.keycloak.sha1hashprovider;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.keycloak.credential.hash.PasswordHashProviderFactory;
import org.keycloak.models.credential.PasswordCredentialModel;

class SHA1HashProviderTest {
	private final SHA1HashProvider provider = new SHA1HashProvider(SHA1HashProviderFactory.ID);

	@Test
	void retainsLegacyEncoding() {
		assertEquals("2aae6c35c94fcfb415dbe95f408b9ce91ee846ed", provider.encode("hello world", 0));
		assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", provider.encode("", 0));
		assertEquals(provider.encode("", 0), provider.encode("", 42));
		assertEquals("042dc4512fa3d391c5170cf3aa61e6a638f84342", provider.encode("i", 0));
	}

	@Test
	void verifiesLegacySaltedCredentialsWithoutLoggingAtAnyLevel() {
		PasswordCredentialModel credential = PasswordCredentialModel.createFromValues(
			"sha1-salted", "hello ".getBytes(StandardCharsets.UTF_8), 42,
			"2aae6c35c94fcfb415dbe95f408b9ce91ee846ed");
		List<LogRecord> records = new ArrayList<>();
		Logger root = Logger.getLogger("");
		Level previousLevel = root.getLevel();
		Handler handler = new Handler() {
			public void publish(LogRecord record) { records.add(record); }
			public void flush() {}
			public void close() {}
		};
		handler.setLevel(Level.ALL);
		root.addHandler(handler);
		root.setLevel(Level.ALL);
		PrintStream stdout = System.out;
		PrintStream stderr = System.err;
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (PrintStream capture = new PrintStream(output, true, StandardCharsets.UTF_8)) {
			System.setOut(capture);
			System.setErr(capture);
			assertTrue(provider.verify("world", credential));
			assertFalse(provider.verify("wrong password", credential));
		} finally {
			System.setOut(stdout);
			System.setErr(stderr);
			root.removeHandler(handler);
			root.setLevel(previousLevel);
		}
		assertTrue(records.isEmpty(), "Verification must not emit log records");
		assertEquals("", output.toString(StandardCharsets.UTF_8));
	}

	@Test
	void retainsEmptyAndUtf8SaltCompatibility() {
		PasswordCredentialModel emptySalt = provider.encodedCredential("hello world", 1);
		assertTrue(provider.verify("hello world", emptySalt));
		assertFalse(provider.verify("wrong", emptySalt));
		PasswordCredentialModel utf8Salt = PasswordCredentialModel.createFromValues(
			"sha1-salted", "sält".getBytes(StandardCharsets.UTF_8), 1,
			"43f45e3ce395a79c35b506f50497e1aecf2ba5d7");
		assertTrue(provider.verify("password", utf8Salt));
	}

	@Test
	void registersTheSameProviderId() {
		PasswordHashProviderFactory factory = ServiceLoader.load(PasswordHashProviderFactory.class)
			.stream().map(ServiceLoader.Provider::get)
			.filter(candidate -> candidate.getId().equals("sha1-salted")).findFirst().orElseThrow();
		assertInstanceOf(SHA1HashProvider.class, factory.create(null));
	}
}
