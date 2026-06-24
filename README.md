# Dynatrace Deployment Webhook Test Repository

Simple Java Spring Boot app to test GitHub deployment webhooks integrated with Dynatrace, including OneAgent instrumentation and OpenPipeline event processing.

## 📋 Overview

This repo demonstrates:
- ✅ GitHub Actions workflow that creates deployments for `dev`, `qa`, `prod` environments
- ✅ GitHub webhook sending deployment events to your Cloud Run proxy
- ✅ Dynatrace OneAgent instrumentation in WSL2
- ✅ REST API with intentional error injection for problem generation
- ✅ Problem metadata enrichment with deployment context

## 🚀 Quick Start

### 1. Clone/Create the Repo

```bash
# Create a new GitHub repo or use an existing one
# Then clone it locally in your WSL2
git clone https://github.com/YOUR-USERNAME/deployment-webhook-test.git
cd deployment-webhook-test
```

Copy all the files from this setup into your repo:
```
├── pom.xml
├── Dockerfile
├── .github/workflows/deploy.yml
├── src/main/java/com/test/dynatrace/
│   ├── DemoApp.java
│   ├── Order.java
│   ├── OrderService.java
│   └── OrderController.java
└── src/main/resources/
    └── application.yml
```

Push to GitHub:
```bash
git add .
git commit -m "Initial deployment webhook test setup"
git push origin main
```

---

### 2. Set Up GitHub Webhook (Point to Your Cloud Run Proxy)

1. Go to your repo → **Settings** → **Webhooks**
2. Click **Add webhook**
3. **Payload URL**: `https://YOUR-CLOUD-RUN-PROXY-URL/webhook` (your Cloud Run endpoint)
4. **Content type**: `application/json`
5. **Events**: Select **Deployments**
6. **Active**: ✅ Check
7. Click **Add webhook**

**Test it:** GitHub will show delivery history in the webhook settings. You can manually re-deliver to see if your proxy is receiving events.

---

### 3. Build the App (in WSL2)

```bash
# In your WSL2 instance
cd /path/to/deployment-webhook-test

# Install Maven if not already installed
sudo apt-get update && sudo apt-get install -y maven openjdk-17-jdk

# Build
mvn clean package
```

Expected output:
```
[INFO] BUILD SUCCESS
[INFO] Total time: X.XXs
[INFO] Finished at: 2026-06-24T...
```

---

### 4. Verify OneAgent in WSL2

**Check OneAgent is running:**

```bash
# Check if OneAgent process is running
ps aux | grep oneagent

# Should see something like:
# root    1234  /opt/dynatrace/oneagent/agent/lib64/liboneagentloader.so
```

**Verify environment is set:**

```bash
# OneAgent sets this environment variable
echo $LD_PRELOAD

# Should contain: /opt/dynatrace/oneagent/agent/lib64/liboneagentloader.so
```

If OneAgent is not running:
1. Download from your Dynatrace environment → **Deployment** → **Linux**
2. Install: `sudo ./dynatrace-oneagent-linux-VERSION.sh`
3. Start: `sudo systemctl start oneagent`

---

### 5. Run the App in WSL2 (OneAgent will monitor it)

```bash
# Run the JAR directly
java -jar target/deployment-webhook-test-1.0.0.jar

# Or use Maven to run directly
mvn spring-boot:run
```

You should see:
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| | | | | | | || (_| |  ) ) ) )
  '  |____| ._ |_| |_|_| |_\__, | / / / /
 =========|_|===========|___/=__|_/_/_/_/

[main] c.t.d.DemoApp              : Started DemoApp in X.XXX seconds
```

The app is now running at `http://localhost:8080`

---

### 6. Trigger a Deployment (Fires the Webhook)

Go to your GitHub repo → **Actions** tab

1. Click **Manual Deployment (Webhook Test)** workflow
2. Click **Run workflow**
3. Select environment: **dev** (or qa/prod)
4. Click **Run workflow**

This will:
- Build the app
- Create a GitHub Deployment for that environment
- Trigger the webhook → sends event to your Cloud Run proxy
- Update deployment status

**Watch for it:**

In your Cloud Run proxy logs, you should see:
```json
{
  "action": "created",
  "deployment": {
    "id": 4851874029,
    "environment": "dev",
    "creator": {...},
    ...
  }
}
```

---

### 7. Generate Traffic & Test Problems

**Trigger intentional errors:**

```bash
# Option 1: Simple error
curl -X POST http://localhost:8080/api/trigger-error

# Option 2: Batch errors
curl -X POST "http://localhost:8080/api/trigger-error/batch?count=10"

# Option 3: Enable random error injection (30% of requests fail)
curl -X POST "http://localhost:8080/api/errors/enable?errorRate=30"

# Then hit normal endpoints - some will fail
curl http://localhost:8080/api/orders
curl http://localhost:8080/api/orders
curl http://localhost:8080/api/orders
```

**Generate traffic for normal monitoring:**

```bash
# List orders
curl http://localhost:8080/api/orders

# Create an order
curl -X POST "http://localhost:8080/api/orders?customerName=TestUser&amount=99.99"

# Get specific order (use ID from create response)
curl http://localhost:8080/api/orders/ORD-XXXXX

# Process order
curl -X POST http://localhost:8080/api/orders/ORD-XXXXX/process

# Update status
curl -X PUT "http://localhost:8080/api/orders/ORD-XXXXX/status?status=completed"
```

---

### 8. Check Dynatrace

1. Go to your Dynatrace environment
2. Look for the application: **deployment-webhook-test**
3. Verify:
   - ✅ Service detected and instrumented
   - ✅ Transactions visible
   - ✅ Errors/problems appearing
   - ✅ Deployment events in **Releases** or custom events (depending on OpenPipeline setup)

**To see custom events:**
- Go to **Events** → **Custom events** (or your OpenPipeline ingestion endpoint)
- Filter by the deployment you triggered
- Check if your Cloud Run proxy successfully sent it

---

## 🛠️ API Endpoints Reference

### Health & Info
- `GET /api/health` → Simple health check

### Orders
- `GET /api/orders` → List all orders
- `GET /api/orders/{orderId}` → Get specific order
- `POST /api/orders?customerName=NAME&amount=AMOUNT` → Create order
- `PUT /api/orders/{orderId}/status?status=STATUS` → Update status
- `POST /api/orders/{orderId}/process` → Process order
- `DELETE /api/orders/{orderId}` → Delete order

### Error Injection
- `POST /api/trigger-error` → Trigger single error
- `POST /api/trigger-error/batch?count=N` → Trigger N errors
- `POST /api/errors/enable?errorRate=30` → Enable random errors (30% rate)
- `POST /api/errors/disable` → Disable random errors
- `GET /api/errors/config` → Get error config status

---

## 📊 Workflow: End-to-End Test

1. **Start the app** → `java -jar target/...jar`
2. **Run health check** → `curl http://localhost:8080/api/health`
3. **Trigger deployment** → GitHub Actions (dev/qa/prod)
4. **Verify webhook fired** → Check Cloud Run proxy logs
5. **Generate traffic** → `curl http://localhost:8080/api/orders` (multiple times)
6. **Inject errors** → `curl -X POST http://localhost:8080/api/trigger-error`
7. **Monitor in Dynatrace**:
   - Service transactions
   - Error rate spikes
   - Deployment context (once integrated with problem metadata)
8. **Verify OpenPipeline** → Check if deployment event reached Dynatrace

---

## 🐳 Running in Docker (Optional)

If you prefer running containerized:

```bash
# Build container
docker build -t deployment-webhook-test:latest .

# Run in WSL2 Docker
# OneAgent from WSL2 host will automatically instrument the container
docker run -p 8080:8080 deployment-webhook-test:latest

# Or with explicit Docker socket mounting for OneAgent
docker run \
  -p 8080:8080 \
  -v /proc:/proc:ro \
  deployment-webhook-test:latest
```

---

## ❓ Troubleshooting

### App won't start
```bash
# Check Java version
java -version  # Should be 17+

# Check port 8080 is free
lsof -i :8080

# Run with debug logging
LOGGING_LEVEL_COM_TEST_DYNATRACE=DEBUG java -jar target/...jar
```

### OneAgent not detecting app
```bash
# Verify OneAgent is running
ps aux | grep oneagent

# Check LD_PRELOAD is set
echo $LD_PRELOAD

# Restart OneAgent
sudo systemctl restart oneagent

# Check OneAgent logs
tail -f /var/log/dynatrace/oneagent/
```

### Webhook not firing
```bash
# Check webhook configuration
# GitHub repo → Settings → Webhooks → Click webhook

# Manually redeliver test event
# Click on a failed/pending delivery → Redeliver

# Check your Cloud Run proxy logs for incoming events
gcloud run logs read YOUR-PROXY-NAME --limit 50
```

### Errors not showing in Dynatrace
```bash
# Wait a minute for events to be processed
# Check Dynatrace service detection
# Go to Services → deployment-webhook-test
# Verify OneAgent version in Dynatrace matches what's running

# Check if errors are being generated
curl -X POST "http://localhost:8080/api/trigger-error/batch?count=5"

# Then hit another endpoint to see errors spike
curl http://localhost:8080/api/orders
```

---

## 🔗 Next Steps

Once this is working:

1. **OpenPipeline Integration**: Enhance your Cloud Run proxy to send deployment events to Dynatrace OpenPipeline
2. **Problem Metadata**: When a problem is detected after a deployment, add deployment context to problem metadata
3. **Multi-Tenant Routing**: Extend the proxy to route events to different Dynatrace tenants based on environment tag
4. **CI/CD Integration**: Integrate this into your actual CD pipeline

---

## 📝 Notes

- The app uses Spring Boot 3.2, Java 17
- OneAgent auto-instruments all HTTP requests and service methods
- Error injection is random-based (when enabled) to simulate realistic failures
- All traffic is captured by OneAgent running on the WSL2 host
- The GitHub workflow uses standard GitHub API calls to create deployments (no external tools needed)

---

Good luck! Let me know when you hit any issues. 🚀
