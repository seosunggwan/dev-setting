(local)
docker-compose up -d --build
docker-compose up -d
./gradlew bootRun
npm run dev

docker-compose build --no-cache
docker-compose up -d

docker-compose up -d --build --no-cache

(prod)
docker-compose -f docker-compose-prod.yml --env-file .env.prod up -d --build

docker-compose -f docker-compose-prod.yml --env-file .env.prod build --no-cache
docker-compose -f docker-compose-prod.yml --env-file .env.prod up -d

docker-compose -f docker-compose-prod.yml --env-file .env.prod up -d --build --no-cache

(경로 이동)
cd "C:\Users\suck6\OneDrive\문서\portfolio"