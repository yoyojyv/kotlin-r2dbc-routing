# spring kotlin r2dbc routing

[이전 R2DBC](https://github.com/yoyojyv/kotlin-webflux-r2dbc) 

r2dbc routing 테스트 목적으로 만듬  

## url (handler)
1. http://localhost:8080/products/{id}
- read test

1. http://localhost:8080/products/saveExample
- write test

## read/write 분리시 라우팅 
- 각 트랜젝션이 걸리도록 TransactionAwareConnectionFactoryProxy 로 ConnectionFactory 를 한번 감싸면 됨 
