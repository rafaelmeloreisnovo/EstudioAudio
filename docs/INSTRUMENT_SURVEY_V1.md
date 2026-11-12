# Instrument Survey V1 — contrato de varredura contextual

**Estado:** `IMPLEMENTED_UNTESTED` no Android físico. Testes host do modelo e compilação
Java não demonstram funcionamento dos sensores de um aparelho.

O painel Instrument Survey observa, durante uma sessão explícita em primeiro plano,
fontes públicas selecionadas pelo usuário. A seleção é organizada por família, grupo e
item. O painel não liga, desliga, sintoniza ou transmite por Wi-Fi, Bluetooth, celular
ou GNSS.

## Fontes e limites

| Fonte | Observação permitida | Limite material |
| --- | --- | --- |
| Sensores Android | eixos de sensores físicos públicos selecionados | presença, taxa e precisão dependem do aparelho; sensores protegidos permanecem indisponíveis |
| GPS/GNSS | posição WGS84, velocidade, rumo, satélites e C/N0 reportados | primeiro plano e localização precisa; coordenada é do telefone, não da torre ou da fonte sonora |
| Celular | identidade pública da célula servidora, tecnologia, canal e sinal | dados podem ser retidos pelo modem; troca de célula encerra a comparabilidade da sessão |
| Wi-Fi | cache de varredura exposto pelo Android | não inicia nova varredura e fixa a primeira rede de referência da sessão |
| Bluetooth | estado do adaptador | sem varredura BLE, pares, tráfego ou alteração do adaptador |
| ServiceMode | somente estado de capacidade | campos privados, ocultos ou específicos do fabricante são `NOT_PUBLIC_API` |

Não há consulta automática a banco externo de torres ou lugares. GPS e rádio podem ser
cruzados no arquivo pela base de tempo monotônica. Isso registra contexto temporal; não
prova a posição física de uma estação celular. Uma futura base espacial deve declarar
fonte, licença, data, cobertura, incerteza e versão antes de ser integrada.

## Qualidade e retenção

Cada linha contém relógio civil, relógio monotônico de recebimento, instante original da
fonte quando disponível, valor, incerteza, setor de orientação e estado. Leituras velhas,
duplicadas, fora de ordem, não finitas, sem precisão GPS ou acima do limite configurado
são preservadas como recusadas e não atualizam os resumos.

A rotação de tela é tratada sem recriar a sessão, permitindo girar o telefone durante a
varredura. Sair do primeiro plano encerra a coleta e remove todos os listeners.

Dados brutos usam um anel limitado por linhas, janela e número de rotações. Resumos por
item e por oito setores mantêm contagem, média, variância amostral e densidade de
observações aceitas. Esses resumos são aprendizado estatístico descritivo da sessão:
não são modelo preditivo, diagnóstico industrial, calibração física nem prova de
estabilidade. Setor desconhecido permanece `-1`.

Ao parar, apenas resumos sem localização, GNSS ou dados de rádio são retidos localmente, junto
com configuração e proveniência. A exportação é uma ação separada. Incluir localização e
rádio exige marcar a opção sensível; a opção padrão os omite. O ZIP usa CRC32 para
integridade de transporte, não autenticação criptográfica.

## Validação necessária

- Teste host adversarial do anel, qualidade, estatística e CSV.
- Compilação Android exata no CI para ARMv7/AArch64 e APK com hash.
- Instalação do mesmo APK em Android 10 e em uma versão moderna.
- Ensaios de permissão negada, aproximada e precisa; GPS desligado; ausência de sensor;
  cache vazio; troca de Wi-Fi e handover celular; pausa e retomada; exportação cheia.
- Comparação dos timestamps e taxas com uma referência conhecida antes de qualquer uso
  industrial ou preditivo.

`R3=<F_ok: contrato, seleção e limites explícitos; F_gap: APK e dispositivo físico NOT_RUN; F_next: CI exato e matriz física sem promover estatística a diagnóstico>`
