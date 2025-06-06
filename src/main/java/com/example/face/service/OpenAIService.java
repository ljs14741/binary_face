package com.example.bitcoin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class OpenAIService {

    private final String apiKey = System.getenv("OPENAI_API_KEY");
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAIService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public String processRequestWithImage(byte[] imageData) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("OpenAI API key is not set. Please set the OPENAI_API_KEY environment variable.");
        }

        String base64Image = Base64.getEncoder().encodeToString(imageData);

//        String promptText = "Hello, you are the face evaluation judge for this 'Laughter Contest.' This contest is a place where people gather to have fun and enjoy some laughs, so there’s no need to take the review too seriously. Please look at the picture and respond in JSON format without code blocks. {\"nickname\": \"A funny nickname, e.g., 'Church Uncle' or 'Fashion Guru'\", \"evaluation\": \"'Oho! Is this a selfie taken in a dark car? The lighting is a bit disappointing, but you took a great photo! Does fashion seem like a comfortable look? This feeling may be 'fashion for going to the convenience store?' ^^' I hope to see more bright expressions next time!', 'Did you pick up the black top from under the bridge? It's tattered.', 'Oh my, the fashion is so plain! It's like a t-shirt you only wear at home. What's with that stern look on your face? I think I've probably lost my smile.'\", \"look_alike_celebrity\": \"A celebrity look-alike suggestion, e.g., 'You have a Yu Jae-suk vibe'\", \"score\": \"A numeric score from 1.0 to 10.0 with one decimal place\"}. Respond in Korean with a fun and varied response!";

        String promptText = """
        You are a face evaluation expert for the 'Visual Contest'. Analyze the provided image and write a humorous, detailed, and engaging evaluation.
        
        Your response MUST be in **valid JSON format** and written in **Korean only**. Do not include any text outside of the JSON format. Avoid using code block symbols (e.g., ```json).
        
        Here is the required JSON format:
        
        {
          "nickname": "A funny nickname in Korean (e.g., 'Lighting Detective', 'Convenience Store King')",
          "evaluation": "A humorous, detailed, and accurate evaluation of the image, focusing on clothing, lighting, background, and facial expressions. Be descriptive, funny, and lighthearted. (e.g., 'Your white shirt brightens up your face! Simple and stylish like a model in an advertisement. A brighter background would have made this perfect!')",
          "look_alike_celebrity": "The name of a celebrity or iconic figure they resemble in Korean (e.g., 'You look like Yoo Jae-suk!')",
          "score": "A numeric score from 1.0 to 10.0, including one decimal place (e.g., '8.7')"
        }
        
        **Examples**:
        1. Nickname: "Lighting Detective", Evaluation: "Your white shirt shines like a spotlight on your face! The background is a bit dark, but the style makes you stand out.", Look-alike: "You look like Yoo Jae-suk!", Score: "9.3"
        2. Nickname: "Convenience Store King", Evaluation: "The relaxed look with a white shirt and casual background makes you look effortlessly cool, like a king of comfort.", Look-alike: "You remind me of Park Seo-joon!", Score: "8.5"
        3. Nickname: "Street Artist", Evaluation: "Your outfit blends perfectly with the street vibes! It's casual yet artistic, like someone ready for a cool photo shoot.", Look-alike: "You have a bit of Gong Yoo's charm!", Score: "9.0"
        
        **Guidelines**:
        1. Write the response ONLY in Korean and valid JSON format.
        2. Do NOT include code block symbols (e.g., ```json).
        3. Avoid gibberish, repeated words, mixed languages, or broken sentences.
        4. Be funny and descriptive but remain logical and respectful.
        5. Focus on clothing, lighting, facial expressions, and background.
        6. Do not add irrelevant details, nonsense words, or unrelated content.
        """;

        Map<String, Object> json = new HashMap<>();
        json.put("model", "gpt-4o-mini");
        json.put("temperature", 1.1);
        json.put("frequency_penalty", 0.3);
        json.put("presence_penalty", 0.2);
        json.put("max_tokens", 400);

        // 메시지 객체 구성 수정
        json.put("messages", new Object[] {
                new HashMap<String, Object>() {{
                    put("role", "user");
                    put("content", new Object[] {
                            new HashMap<String, String>() {{
                                put("type", "text");
                                put("text", promptText);
                            }},
                            new HashMap<String, Object>() {{
                                put("type", "image_url");
                                // image_url 부분을 문자열이 아닌 객체로 감싸서 URL 형태로 설정
                                put("image_url", new HashMap<String, String>() {{
                                    put("url", "data:image/jpeg;base64," + base64Image); // 객체로 수정
                                }});
                            }}
                    });
                }}
        });

        String jsonPayload = objectMapper.writeValueAsString(json);

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .post(RequestBody.create(jsonPayload, MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorResponse = response.body().string();
                log.error("OpenAI API Error: " + errorResponse);

                // **🔹 429 에러 감지**
                if (response.code() == 429 && errorResponse.contains("insufficient_quota")) {
                    return "{\"error\": \"insufficient_quota\"}"; // JSON 형식으로 반환
                }

                throw new IOException("Unexpected code " + response + ": " + errorResponse);
            }

            return response.body().string();
        }
    }
}
