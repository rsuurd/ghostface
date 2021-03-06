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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.springframework.http.HttpMethod.GET;

@Slf4j
@Service
public class TwitchService {
	private RestOperations restOperations;

	private String clientId;

	@Autowired
	public TwitchService(RestOperations restOperations, @Value("${twitch.clientId}") String clientId) {
		this.restOperations = restOperations;

		this.clientId = clientId;
	}

	public Optional<Stream> getStream(String streamer) {
		Objects.requireNonNull(streamer, "Streamer is required");

		String url = String.join("/", "https://api.twitch.tv/kraken/streams", streamer);

		HttpHeaders headers = new HttpHeaders();
		headers.add("Client-ID", clientId);

		Stream stream;

		try {
			stream = restOperations.exchange(url, GET, new HttpEntity<>(headers), StreamResponse.class).getBody().getStream();
		} catch (Exception exception) {
			log.warn("could not get stream", exception);

			stream = null;
		}

		return Optional.ofNullable(stream);
	}

	public boolean userExists(String streamer) {
		Objects.requireNonNull(streamer, "Streamer is required");

		String url = String.join("/", "https://api.twitch.tv/kraken/channels", streamer);

		HttpHeaders headers = new HttpHeaders();
		headers.add("Client-ID", clientId);

		boolean exists;

		try {
			exists = restOperations.exchange(url, GET, new HttpEntity<>(headers), Map.class).getBody().containsKey("_id");
		} catch (Exception exception) {
			exists = false;
		}

		return exists;
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@AllArgsConstructor(access = AccessLevel.PACKAGE)
	static class StreamResponse {
		private Stream stream;

	}

	@Getter
	@AllArgsConstructor(access = AccessLevel.PACKAGE)
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	public static class Stream {
		@JsonProperty("video_height")
		private int videoHeight;

		private Preview preview;

		public int getVideoWidth() {
			return (getVideoHeight() / 9) * 16;
		}
	}

	@Getter
	@AllArgsConstructor(access = AccessLevel.PACKAGE)
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	public static class Preview {
		private String large;
		private String template;
	}
}

