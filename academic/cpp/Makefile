
CXX ?= g++
CXXFLAGS ?= -std=c++17 -Wall -Wextra -Wpedantic -O2 -Iinclude
BUILD := build
FONTES := src/lista_estoque.cpp src/fila_requisicoes.cpp src/pilha_historico.cpp

.PHONY: tudo testar limpar

tudo: $(BUILD)/runner

$(BUILD):
	mkdir -p $(BUILD)

$(BUILD)/runner: $(BUILD) $(FONTES) src/runner.cpp include/*.hpp
	$(CXX) $(CXXFLAGS) $(FONTES) src/runner.cpp -o $(BUILD)/runner

$(BUILD)/testes: $(BUILD) $(FONTES) tests/testes.cpp include/*.hpp
	$(CXX) $(CXXFLAGS) -g -fsanitize=address,undefined $(FONTES) tests/testes.cpp -o $(BUILD)/testes

testar: $(BUILD)/testes
	./$(BUILD)/testes

limpar:
	rm -rf $(BUILD)
