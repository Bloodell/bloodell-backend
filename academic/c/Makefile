CC ?= cc
CFLAGS ?= -std=c11 -Wall -Wextra -Wpedantic -O2 -Iinclude
BUILD := build
FONTES := src/contratos.c src/lista_estoque.c src/fila_requisicoes.c src/pilha_historico.c

# A sondagem precisa linkar, não só compilar: o MinGW (Windows) aceita a flag,
# mas não traz libasan/libubsan e falharia apenas no link.
SANITIZER_FLAGS := $(shell printf 'int main(void){return 0;}\n' | $(CC) -std=c11 -fsanitize=address,undefined -x c - -o sondagem-sanitizer.tmp >/dev/null 2>&1 && printf '%s' '-fsanitize=address,undefined'; rm -f sondagem-sanitizer.tmp sondagem-sanitizer.tmp.exe)
TEST_CFLAGS := $(CFLAGS) -g $(SANITIZER_FLAGS)

.PHONY: tudo testar limpar

tudo: $(BUILD)/runner

$(BUILD):
	mkdir -p $(BUILD)

$(BUILD)/runner: $(BUILD) $(FONTES) src/runner.c include/*.h
	$(CC) $(CFLAGS) $(FONTES) src/runner.c -o $(BUILD)/runner

$(BUILD)/testes: $(BUILD) $(FONTES) tests/testes.c include/*.h
	$(CC) $(TEST_CFLAGS) $(FONTES) tests/testes.c -o $(BUILD)/testes

testar: $(BUILD)/testes
	# O sandbox executa sob ptrace; nele o LeakSanitizer aborta antes do teste.
	# AddressSanitizer e UndefinedBehaviorSanitizer continuam ativos.
	ASAN_OPTIONS=detect_leaks=0 ./$(BUILD)/testes

limpar:
	rm -rf $(BUILD)
