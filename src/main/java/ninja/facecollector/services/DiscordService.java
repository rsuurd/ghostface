/*
 *     Face Collector
 *     Copyright (C) 2018 Rolf Suurd
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package ninja.facecollector.services;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@Slf4j
@Service
public class DiscordService {
	private RestOperations restOperations;

	private String baseUrl;
	private String clientId;
	private String token;

	@Autowired
	public DiscordService(RestOperations restOperations, @Value("${baseUrl}") String baseUrl, @Value("${discord.clientId}") String clientId, @Value("${discord.token}") String token) {
		this.restOperations = restOperations;

		this.baseUrl = baseUrl;
		this.clientId = clientId;
		this.token = token;
	}

	public void publishEmoji(String name, byte[] data, String guildId) {
		Objects.requireNonNull(name, "Name is required");
		Objects.requireNonNull(data, "Image is required");
		Objects.requireNonNull(guildId, "Guild-ID is required");

		Map<String, Object> emoji = new LinkedHashMap<>();
		emoji.put("name", name);
		emoji.put("image", "data:image/png;base64," + Base64.getEncoder().encodeToString(data));

		deleteEmoji(name, guildId);

		try {
			restOperations.exchange(EMOJI_URL, POST, new HttpEntity<>(emoji, createBotHeaders()), Void.class, guildId);
		} catch (Exception exception) {
			log.error("{}'s emoji publication to {} failed", name, guildId, exception);
		}
	}

	public void deleteEmoji(String name, String guildId) {
		Objects.requireNonNull(name, "Name is required");
		Objects.requireNonNull(guildId, "Guild-ID is required");

		findEmojiId(name, guildId).ifPresent(emojiId ->
			restOperations.exchange(EMOJI_URL + "/{emojiId}", DELETE, new HttpEntity<>(createBotHeaders()), Void.class, guildId, emojiId));
	}

	private Optional<String> findEmojiId(String name, String guildId) {
		Objects.requireNonNull(name, "Name is required");
		Objects.requireNonNull(guildId, "Guild-ID is required");

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> emojis = restOperations.exchange(EMOJI_URL, GET, new HttpEntity<>(createBotHeaders()), List.class, guildId).getBody();

		return emojis.stream().filter(emoji -> name.equals(emoji.get("name"))).findFirst().map(emoji -> emoji.get("id").toString());
	}

	@Cacheable("users")
	public User getUser(String token) {
		Objects.requireNonNull(token, "Access token required");

		return restOperations.exchange(BASE_URL + "/users/@me", GET, new HttpEntity<>(createHeaders("Bearer", token)), User.class).getBody();
	}

	@Cacheable("guilds")
	public List<Guild> listGuilds(String token) {
		Objects.requireNonNull(token, "Access token required");

		List<Guild> guilds = restOperations.exchange(BASE_URL + "/users/@me/guilds", GET, new HttpEntity<>(createHeaders("Bearer", token)), new ParameterizedTypeReference<List<Guild>>() {}).getBody();

		return guilds.stream().filter(Guild::isOwner).collect(Collectors.toList());
	}

	public String getAddBotUri() {
		return UriComponentsBuilder.fromUriString("https://discordapp.com/api/oauth2/authorize")
				.queryParam("client_id", clientId)
				.queryParam("permissions", 1073741824)
				.queryParam("redirect_uri", baseUrl)
				.queryParam("scope", "bot")
			.build().toUriString();
	}

	private HttpHeaders createBotHeaders() {
		return createHeaders("Bot", token);
	}

	private HttpHeaders createHeaders(String bearer, String token) {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.AUTHORIZATION, String.join(" ", bearer, token));
		headers.add(HttpHeaders.USER_AGENT, "DiscordBot (https://github.com/rsuurd/face-collector, ∞)");

		return headers;
	}

	private static final String BASE_URL = "https://discordapp.com/api";
	private static final String EMOJI_URL = BASE_URL.concat("/guilds/{serverId}/emojis");

	@Data
	public static class User {
		private String id;
		private String username;
		private String avatar;
	}

	@Data
	public static class Guild {
		private String id;
		private String name;
		private String icon;
		private boolean owner;
	}
}
