# SilverTV

App Android simples para assistir aos canais brasileiros abertos que transmitem pela internet.
A lista de canais vem do projeto público **iptv-org**
(`https://iptv-org.github.io/iptv/countries/br.m3u`) e é baixada sempre que o app abre,
então novos canais aparecem sem precisar recompilar.

## Recursos
- Lista de canais com logo, busca (sem diferenciar acentos) e filtro por categoria
- Favoritos (☆)
- Player em tela cheia (Media3/ExoPlayer, HLS)
- Deslize para os lados ou use ⏮ ⏭ para trocar de canal
- Se um canal estiver fora do ar: "Tentar de novo" ou "Próximo canal"
- Funciona com a última lista salva se a internet da lista falhar
- ⚙ permite trocar o endereço da lista M3U (por exemplo, para uma lista sua)

## Como compilar pelo GitHub
1. Crie um repositório novo e envie **todo o conteúdo desta pasta** pelo site (Add file → Upload files).
2. A pasta `.github` não sobe pelo upload. Então, no site do GitHub, clique em
   **Add file → Create new file**, digite o nome
   `.github/workflows/build.yml`
   e cole o conteúdo do arquivo `workflow-github/build.yml`. Clique em **Commit**.
3. Vá em **Actions** → aguarde o build (≈ 5 min) → baixe o artefato **SilverTV-apk**.
4. Descompacte e instale o `app-debug.apk` no celular (permita "instalar apps desconhecidos").

Observação: cada build gera uma assinatura de teste nova. Se o Android recusar a atualização,
desinstale a versão anterior e instale de novo (os favoritos são apagados nesse caso).

## Avisos
- A disponibilidade dos canais depende das emissoras: alguns ficam fora do ar, mudam de link
  ou são bloqueados por região. A lista do iptv-org é atualizada pela comunidade.
- O app usa dados móveis quando fora do Wi-Fi (vídeo ao vivo consome bastante).
