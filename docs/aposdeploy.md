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

Isso mostra o commit mais recente no remoto. Para ver o commit que está rodando no container, você pode verificar quando a imagem foi construída:
cd /opt/pitstop
git log --oneline -1 origin/main


Para rodar os testes localmente quando quiser:                                                                                                                                                                                                                                                                                                                                                                                  
./mvnw test                           # Todos os testes                                                                                                                                                                                                                                                                                                                                                                         
./mvnw test -Dtest=ClienteServiceTest # Teste específico    


URL: http://localhost:5173/login

Credenciais:
- Email: admin@pitstop.local
- Senha: password    
- 