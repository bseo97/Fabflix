# Fabflix Project 5 - Scalable Deployment with Docker and Kubernetes

## General

#### Names: Brian Seo, Lucas Kim

#### Project 5 Video Demo Link:
https://youtu.be/z5wcuFpePEw

#### Instruction of Deployment:
1. Set up a Kubernetes cluster on AWS using kOps with 1 control-plane and 3 worker nodes.
2. Deploy a single MySQL server to handle both master and slave configurations.
3. Monolithic version: Use `kubectl apply -f fabflix.yaml` and `kubectl apply -f ingress.yaml` to deploy the Fabflix application.
4. Access the Fabflix site using the AWS ELB URL.
5. For multi-service architecture, stop the original deployment using `kubectl scale`, then apply `fabflix-multi.yaml` and `ingress-multi.yaml`.

#### Collaborations and Work Distribution:
- **Brian Seo**: Kubernetes ingress configuration, JWT-based session architecture, MySQL read-write splitting, cart and order processing, Docker image publishing, cluster debugging, full-stack integration, and final video demo.
- **Lucas Kim**: Docker setup, static asset routing, test deployment pipeline, pod logging validation.

---

## Task 1: Run the Fabflix application in a Docker container

- Followed Murphy Movies "Docker" branch to build and verify Fabflix as a Docker image.
- Created two images:
  - `fabflix-login`: handles user authentication and session
  - `fabflix-movies`: handles browsing, search, cart, checkout
- Docker images pushed to Docker Hub:  
  - `bseo97/fabflix-login:v3`  
  - `bseo97/fabflix-movies:v3`  

---

## Task 2: Set up a Kubernetes (K8s) cluster on AWS

- Created Kubernetes cluster on AWS using `kOps`.
- Cluster consisted of 1 control-plane and 3 worker nodes.
- Verified cluster status using `kops get all` and `kubectl get nodes`.

---

## Task 3: Deploy Fabflix to the K8s cluster

- Wrote `fabflix.yaml` and `ingress.yaml` to deploy Fabflix application.
- Moved MySQL database into the cluster using StatefulSet (MySQL master + slave).
- Verified:
  - Functional login
  - Movie browsing and search
  - Cart and order placement
- Confirmed sales data was inserted into MySQL by querying the database from within a pod.

---

## Task 4: Revise Fabflix using a multi-service architecture

- Split the Fabflix app into two services:
  - `/api/login`, `/api/logout`, `/api/session-check` → `fabflix-login`
  - All other `/api/...` and static files → `fabflix-movies`
- Used Maven multi-profile system to compile separate `.war` files.
- Wrote `fabflix-multi.yaml` and `ingress-multi.yaml`:
  - Set Ingress rules to route requests to the appropriate service
  - Configured session stickiness using cookies
- Verified:
  - JWT cookie is issued and stored on login
  - Requests to `/api/login` are handled by login pod
  - Other endpoints (search, browse, cart, place order) handled by movie pods
  - Logs confirm correct routing per service

---

## Files Included in the Monolithic Directory

- `Dockerfile`
- `fabflix.yaml`
- `ingress.yaml`
- `pom.xml`

## Files Included in the multi-service Directories

- `Dockerfile`
- `fabflix-multi.yaml`
- `ingress-multi.yaml`
- `pom.xml`


---

## Demo Checklist (Video Demonstration Includes):

- AWS EC2 cluster setup shown (control + worker nodes)
- Docker Hub with both images verified
- `kops get all` and `kubectl get all` executed
- Application tested end-to-end:
  - User login
  - Movie search and add to cart
  - Checkout and DB insertion confirmed via `kubectl exec mysql-secondary-0`
- JWT verified in browser cookies
- Multi-service routing confirmed by analyzing pod logs

---

## Notes

- ELB URL from Ingress remains consistent between single-service and multi-service deployments.
- Cluster and resources were removed after demo to prevent charges.
- HTTPS and reCAPTCHA were considered optional and were not included.

