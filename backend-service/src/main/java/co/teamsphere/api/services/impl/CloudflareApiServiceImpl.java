package co.teamsphere.api.services.impl;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import co.teamsphere.api.response.CloudflareApiResponse;
import co.teamsphere.api.services.CloudflareApiService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CloudflareApiServiceImpl implements CloudflareApiService {
    @Value("${cloudflare.api.key}")
    private String cloudflareApiKey;
    @Value("${cloudflare.api.accountID}")
    private String cloudflareAccountID;
    private final String apiUrl = "https://api.cloudflare.com/client/v4/accounts/{account_id}/images/v1";
    private final RestTemplate restTemplate;

    public CloudflareApiServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public CloudflareApiResponse uploadImage(MultipartFile imageFile) throws IOException  {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Authorization", String.format("Bearer %s", cloudflareApiKey));

        var body = new LinkedMultiValueMap<String, Object>();

        body.add("file", new ByteArrayResource(imageFile.getBytes()) {
            @Override
            public String getFilename() {
                return UUID.randomUUID().toString();
            }
        });

        body.add("requireSignedURLs", false);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        String url = apiUrl.replace("{account_id}", cloudflareAccountID);

        CloudflareApiResponse cloudflareApiResponse = new CloudflareApiResponse();
        ResponseEntity<CloudflareApiResponse> response = null;

        try {
            log.info("Sending Image request to URL: {}", url);
            response = restTemplate.postForEntity(url, requestEntity, CloudflareApiResponse.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("{} uploaded successfully to Cloudflare.", body.get("file"));
                return response.getBody();
            }
            log.error("Failed to upload image to Cloudflare. Status code: {}", response.getStatusCode());
        } catch (HttpClientErrorException e){
            assert response != null;
            cloudflareApiResponse.setErrors(Objects.requireNonNull(response.getBody()).getErrors());
            log.error("Cloudflare API error: {}", e.getResponseBodyAsString());
        }

        return cloudflareApiResponse;
    }

    @Override
    public CloudflareApiResponse deleteImage(String imageID) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", String.format("Bearer %s", cloudflareApiKey));

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
    
        String url = apiUrl.replace("{account_id}", cloudflareAccountID) + "/" + imageID;
    
        CloudflareApiResponse cloudflareApiResponse = new CloudflareApiResponse();
    
        try {
            log.info("Sending DELETE request to URL: {}", url);
            ResponseEntity<CloudflareApiResponse> response = restTemplate.exchange(
                url, 
                org.springframework.http.HttpMethod.DELETE, 
                requestEntity, 
                CloudflareApiResponse.class
            );
    
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("ImageID: {} deleted successfully from Cloudflare.");
                return response.getBody();
            }
            log.error("Failed to delete image from Cloudflare. Status code: {}", response.getStatusCode());
        } catch (HttpClientErrorException e) {
            log.error("Cloudflare API error: {}", e.getResponseBodyAsString());
            cloudflareApiResponse.setErrors(List.of(e.getMessage()));
        }
    
        return cloudflareApiResponse;
    }
}
    