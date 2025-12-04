/* Limpa a tabela e reinicia a contagem de ID a cada vez que o bot inicia */
TRUNCATE TABLE pizzas RESTART IDENTITY;

/* Insere o cardápio base */
INSERT INTO pizzas (nome, descricao, preco) VALUES 
('Calabresa', 'Molho de tomate, queijo muçarela e calabresa fatiada.', 50.00),
('Mucarela', 'Molho de tomate, queijo muçarela e orégano.', 45.00),
('Frango com Catupiry', 'Molho de tomate, queijo, frango desfiado e catupiry.', 52.00),
('Portuguesa', 'Molho, presunto, ovo, cebola e muçarela.', 53.00);

TRUNCATE TABLE bebidas RESTART IDENTITY CASCADE;

INSERT INTO bebidas (nome, preco, litros) VALUES
('Coca Cola G', 12.00, 'dois litros'),
('Guarana', 10.00, 'dois litros'),
('Agua', 4.00, '500ml' ),
('Coca Cola P', 6.00, '600ml');
