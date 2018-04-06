package ninja.facecollector.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;

import java.util.Objects;
import java.util.Optional;

import static org.springframework.http.HttpMethod.GET;

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

		return Optional.ofNullable(restOperations.exchange(url, GET, new HttpEntity<>(headers), StreamResponse.class).getBody().getStream());
	}

	@Getter
	private static class StreamResponse {
		private Stream stream;
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	public static class Stream {
		@JsonProperty("video_height")
		private int videoHeight;

		private Preview preview;

		public Stream(int videoHeight, Preview preview) {
			this.videoHeight = videoHeight;
			this.preview = preview;
		}

		public int getVideoWidth() {
			return (getVideoHeight() / 9) * 16;
		}
	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	public static class Preview {
		private String large;
		private String template;

		public Preview(String large, String template) {
			this.large = large;
			this.template = template;
		}
	}
}

