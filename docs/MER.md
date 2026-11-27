# Modelo Entidade Relacionamento

O sistema representa avaliações de aprendizagem, com pontuação por questão, correção automática das objetivas e correção manual das respostas textuais. O modelo corresponde ao [schema PostgreSQL](../database/schema.sql).

## Entidades e atributos

Sublinhar o identificador de cada entidade ao desenhar. Os nomes entre parênteses ajudam a relacionar o conceito ao SQL.

| Entidade | Identificador | Atributos |
|---|---|---|
| Usuário (`usuarios`) | id | nome, email, senha_hash, perfil, criado_em |
| Tipo de questão (`tipos_questao`) | id | codigo |
| Questão (`questoes`) | id | enunciado, criada_em |
| Alternativa (`alternativas`) | id | texto, ordem, correta |
| Avaliação (`avaliacoes`) | id | titulo, descricao, inicio_em, fim_em, status, criada_em |
| Item da avaliação (`avaliacao_questoes`) | id | ordem, valor |
| Participação (`participacoes`) | id | iniciada_em, entregue_em |
| Resposta (`respostas`) | id | texto_resposta, nota_obtida, respondida_em |

As referências entre entidades são os relacionamentos abaixo. No desenho conceitual, não é necessário repetir as FKs como atributos. Elas estão explicitadas no DER lógico.

## Relacionamentos e cardinalidades

Nesta tabela, “A → B” indica quantos B cada A pode ter; a última coluna indica quantos A cada B tem.

| A | Relacionamento | B | Cada A possui B | Cada B possui A |
|---|---|---|---|---|
| Usuário | escreve | Questão | 0 a N | 1 |
| Usuário | organiza | Avaliação | 0 a N | 1 |
| Tipo de questão | classifica | Questão | 0 a N | 1 |
| Questão | oferece | Alternativa | 0 a N | 1 |
| Avaliação | contém | Item da avaliação | 0 a N | 1 |
| Questão | aparece em | Item da avaliação | 0 a N | 1 |
| Avaliação | recebe | Participação | 0 a N | 1 |
| Usuário | realiza | Participação | 0 a N | 1 |
| Participação | registra | Resposta | 0 a N | 1 |
| Item da avaliação | recebe | Resposta | 0 a N | 1 |
| Alternativa | é selecionada em | Resposta | 0 a N | 0 a 1 |

Usuário exerce o papel de professor em “escreve” e “organiza”, e o papel de aluno em “realiza”. Esses papéis são validados pelo backend; a FK isoladamente só garante que o usuário existe. O atributo perfil admite PROFESSOR ou ALUNO. Não há tabelas separadas de professor e aluno no sistema atual.

## Associações importantes

Avaliação e Questão têm relação N:N, resolvida por Item da avaliação. Ordem e valor pertencem ao item: a mesma questão pode ter valores diferentes em avaliações diferentes. Cada par avaliação/questão é único.

Usuário no papel de aluno e Avaliação têm relação N:N, resolvida por Participação. Cada par aluno/avaliação é único, permitindo uma tentativa por aluno na prova.

Resposta liga uma participação a um item da avaliação. Cada par participação/item é único. Não desenhar uma ligação direta adicional de Resposta com Usuário ou Questão: essas informações já são obtidas pelos relacionamentos existentes.

## Regras para anotar ao lado do desenho

1. Questões de texto livre não possuem alternativas. No cadastro completo pela aplicação, objetivas possuem ao menos duas alternativas e exatamente uma correta. O SQL isolado garante no máximo uma correta.
2. Rascunhos podem não ter itens nem participantes; por isso as cardinalidades gerais usam zero. Para publicar, a aplicação exige ao menos uma questão e um aluno.
3. Uma resposta é de alternativa ou de texto, nunca ambas. Uma resposta objetiva seleciona exatamente uma alternativa da própria questão; uma textual não seleciona alternativa.
4. Participação e item de uma resposta devem pertencer à mesma avaliação. Essa condição também é verificada por trigger.
5. Questão em branco não gera registro em Resposta. Nota nula indica correção pendente; zero indica nota atribuída.
6. A nota fica entre zero e o valor do item. O total da avaliação é calculado, não armazenado na participação.
7. Fim da avaliação é posterior ao início; entrega exige início da participação e ocorre no mesmo instante ou depois dele.
