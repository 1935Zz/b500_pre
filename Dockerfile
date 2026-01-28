FROM eclipse-temurin:17-jdk

# Install basic development tools
RUN apt-get update && \
    apt-get install -y \
    maven \
    git \
    curl \
    vim \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /workspace

# Copy project files
COPY . .

# Download Maven dependencies (but don't build the app)
RUN mvn dependency:resolve dependency:resolve-plugins || true

# Default to interactive shell
CMD ["/bin/bash"]