# Energy App - Backend

Backend service for the Energy App, responsible for monitoring the UK energy mix and calculating optimal charging windows. Built with Java Spring Boot 3 and Docker.

## 🛠 Technologies
* **Language:** Java 17
* **Framework:** Spring Boot 3
* **Containerization:** Docker
* **API Documentation:** Swagger / OpenAPI
* **Build Tool:** Maven

## 🚀 Local Execution (Docker)

To run the application locally using Docker:

1. **Build the image:**
   ```bash
   docker build -t energy-app-backend .
   ```

2. **Run the container:**
   ```bash
   docker run -p 8080:8080 energy-app-backend
   ```
   The server will start at: `http://localhost:8080`

## 📚 API Documentation (Swagger)
The application includes integrated OpenAPI documentation. Once running, you can access it at:
👉 `http://localhost:8080/swagger-ui/index.html`

## 🌍 Deployment (Render.com)
The application is configured to run as a **Web Service** on Render (Docker Runtime).

**Required Environment Configuration:**
1. Create a new Web Service pointing to this repository.
2. In the *Environment Variables* tab, add:
    * `PORT` = `8080` (Spring Boot listens on this port).
3. **Important:** After deployment, note down the **URL address** (public address, e.g., `https://energy-app-frontend-xyz.onrender.com`). You will need this to configure the Frontend connection.