Agora, nos próximos deploys, basta executar:

./deploy.sh           # Deploy completo
./deploy.sh backend   # Apenas backend
./deploy.sh frontend  # Apenas frontend

docker exec pitstop-redis redis-cli FLUSHALL


Opção 1 - Atualizar manualmente uma vez o script:
ssh root@VPS
cd /opt/pitstop
cp deploy/deploy.sh ./deploy.sh
chmod +x deploy.sh


O cache Redis será limpo automaticamente, evitando erros 500 por cache corrompido.



localmente
docker exec pitstop-redis redis-cli FLUSHALL