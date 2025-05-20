# Teamsphere Backend
Welcome to the backend of Teamsphere, a chat application built with Spring Boot. This application provides the core functionality for one-on-one chats, user authentication, and profile image storage. Future updates will include group chat functionality using web sockets with the STOMP protocol.

## Table of Contents

Features
- [Technologies Used](#technologies-used)
- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Installation](#installation)
    - [Configuration](#configuration)
    - [Running the Application](#configuration)
- [API Documentation](#to-be-added-soon)
- [Deployment](#to-be-added-soon)
- [Contributing](#contributing)
- [License](#license)

## Features
- User authentication with JWT using Spring Security
- One-on-one chat functionality
- Profile image storage using Cloudflare Images
- MySQL database integration
- Docker Compose for building and deployment
- Hosted on Digital Ocean

## Technologies Used
- Spring Boot: Application framework
- Spring Security: For authentication and authorization
- JWT (JSON Web Tokens): For securing API endpoints
- MySQL: Database for storing user and chat information
- Cloudflare Images: For storing and serving profile images
- Docker Compose: For containerizing and deploying the application
- Digital Ocean: Hosting provider

## Getting Started
### Prerequisites
Before you begin, ensure you have the following installed on your system:

- Java 21 or higher
- Maven
- Docker and Docker Compose
- MySQL

### Installation
1. Clone the repository:

    ```shell
    git clone https://github.com/Flanderzz/TeamSphere-API.git
    cd TeamSphere-API/YipYapTimeAPI
    ```

2. Build the application using Maven:
    ```shell
    mvn clean install
    ```
### Configuration
1. Generate RSA Token in resource folder

### Create key pair
Keys smaller than 2048 bits are considered unsecure
```bash
openssl genrsa -out keypair.pem {your size here}
```

### Extract public key
```bash
openssl rsa -in keypair.pem -pubout -out public.pem
```

### Extract private key
```bash
openssl pkcs8 -in keypair.pem -topk8 -nocrypt -inform PEM -outform PEM -out private.pem
```

2. Update `application-local.yml` with your own values for the cloudflare section here is a link on how to get started with [cloudflare images.](https://developers.cloudflare.com/images/get-started/)
```yaml
  jwt:
    public:
      key: classpath:public.key
    private:
      key: classpath:private.key
    audience: "your audience here"
    cloudflare:
      api:
        accountID: get-this-from-cloudflare
        key: get-this-from-cloudflare
    server:
      port: 5454
    spring:
      datasource:
        driver-class-name: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://local:3306/teamsphere_db
        password: my-secret-pw
        username: root
      jpa:
        database: mysql
        hibernate:
          ddl-auto: update
        show-sql: true
      servlet:
        multipart:
          max-file-size: 20MB
          max-request-size: 20MB
```

### Docker compose
  This is for the main service without the grafanan loki and promtail
```yaml
    version: '3'
    services:
      mysql:
        image: mysql:latest
        environment:
          MYSQL_ROOT_PASSWORD: my-secret-pw
          MYSQL_DATABASE: teamsphere_db
        ports:
          - "3306:3306"
```

## M-Chip MAC users
   If you are on mac with M silicone then you need to update the image in the docker file to
    ```FROM arm64v8/eclipse-temurin:21 as build ```
### To be added soon
1. API Documentation
2. Deployment

## Contributing
We welcome contributions! Please fork the repository and submit pull requests. For major changes, please open an issue first to discuss what you would like to change.
1. Fork the Project
2. Create your Feature Branch (git checkout -b feature/AmazingFeature)
3. Commit your Changes (git commit -m 'Add some AmazingFeature')
4. Push to the Branch (git push origin feature/AmazingFeature)
5. Open a Pull Request

## License
This project is licensed under the MIT License.
