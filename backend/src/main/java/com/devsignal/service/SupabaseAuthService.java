package com.devsignal.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.devsignal.config.SupabaseProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SupabaseAuthService {

	private final SupabaseProperties properties;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;

	public SupabaseAuthService(SupabaseProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.httpClient = HttpClient.newHttpClient();
	}

	public Optional<AuthenticatedSupabaseUser> resolveAuthenticatedUser(String authorizationHeader) {
		if (authorizationHeader == null || authorizationHeader.isBlank()) {
			return Optional.empty();
		}
		if (!properties.isConfigured()) {
			throw new ResponseStatusException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					"Supabase auth is not configured on the backend.");
		}

		String token = extractBearerToken(authorizationHeader);

		HttpRequest request = HttpRequest.newBuilder(URI.create(properties.authUserEndpoint()))
				.header("apikey", properties.publishableKey().strip())
				.header("Authorization", "Bearer " + token)
				.GET()
				.build();

		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				SupabaseUserPayload payload = objectMapper.readValue(response.body(), SupabaseUserPayload.class);
				if (payload.id() == null) {
					throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Supabase token did not include a user id.");
				}
				return Optional.of(new AuthenticatedSupabaseUser(payload.id(), payload.email()));
			}
			if (response.statusCode() == 401 || response.statusCode() == 403) {
				throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Supabase session token.");
			}
			throw new ResponseStatusException(
					HttpStatus.BAD_GATEWAY,
					"Could not verify Supabase session token.");
		}
		catch (IOException | InterruptedException ex) {
			if (ex instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw new ResponseStatusException(
					HttpStatus.BAD_GATEWAY,
					"Failed to verify Supabase session token.",
					ex);
		}
	}

	public AuthenticatedSupabaseUser requireAuthenticatedUser(String authorizationHeader) {
		return resolveAuthenticatedUser(authorizationHeader)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.UNAUTHORIZED,
						"Sign in is required to use DevSignal AI features."));
	}

	private static String extractBearerToken(String authorizationHeader) {
		String trimmed = authorizationHeader.strip();
		if (!trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization header must use Bearer token format.");
		}
		String token = trimmed.substring(7).strip();
		if (token.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization bearer token is empty.");
		}
		return token;
	}

	public record AuthenticatedSupabaseUser(UUID id, String email) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record SupabaseUserPayload(UUID id, String email) {
	}
}
