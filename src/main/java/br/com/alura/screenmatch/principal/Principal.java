package br.com.alura.screenmatch.principal;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collector;
import java.util.stream.Collectors;

//import org.springframework.format.annotation.DateTimeFormat;

import br.com.alura.screenmatch.model.DadosEpisodio;
import br.com.alura.screenmatch.model.DadosSerie;
import br.com.alura.screenmatch.model.DadosTemporada;
import br.com.alura.screenmatch.model.Episodio;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConverteDados;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoApi consumoApi = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();

    private final String ENDERECO = "http://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=43b972a";
    
    public void exibeMenu(){
        
        //Criando o buscador na API OMDB
        System.out.println("Digite o nome da série que desejas buscar");
        var nomeSerie = leitura.nextLine();
		var json = consumoApi.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        
		DadosSerie dadosSerie = conversor.obterDados(json, DadosSerie.class);
        System.out.println(dadosSerie);

        List<DadosTemporada> temporadas = new ArrayList<>();

		for (int i = 1; i<=dadosSerie.totalTemporadas(); i++) {
			json = consumoApi.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + "&season=" + i + API_KEY);
			DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
			temporadas.add(dadosTemporada);
		}
		
        temporadas.forEach(System.out::println);

        /*for (int i = 0; i < dadosSerie.totalTemporadas(); i++){
            List<DadosEpisodio> episodiosTemporada = temporadas.get(i).episodios();
            for (int j = 0; j < episodiosTemporada.size(); j++){
                System.out.println(episodiosTemporada.get(j).titulo());
            }
        }*/

        //Substituição de for pelos lambdas
        //Episodio((parametro) -> expressao)
        temporadas.forEach(t -> t.episodios().forEach(e -> System.out.println(e.titulo())));

        List<DadosEpisodio> dadosEpisodios = temporadas.stream()
            .flatMap(t -> t.episodios().stream())
            .collect(Collectors.toList());

        //Criando um filtro de top5 episódios de acordo com a nota do IMDB    
        System.out.println("\nTop 5 episódios:");
        dadosEpisodios.stream()
            .filter(e -> !e.avaliacao().equalsIgnoreCase("N/A"))
            .sorted(Comparator.comparing(DadosEpisodio::avaliacao).reversed())
            .limit(5)
            .forEach(System.out::println);
        
        List<Episodio> episodio = temporadas.stream()
            .flatMap(t -> t.episodios().stream())
            .map(d -> new Episodio(d.numero(), d))
            .collect(Collectors.toList());
        
        episodio.forEach(System.out::println);

        //Criando um buscador que diz qual temporada o episódio buscado pertence
        System.out.println("Digite um trecho do nome do episódio que está buscando");
        var trechoTitulo = leitura.nextLine();
        
        Optional<Episodio> episodioBuscado = episodio.stream()
            .filter(e -> e.getTitulo().toUpperCase().contains(trechoTitulo.toUpperCase()))
            .findFirst();

        if(episodioBuscado.isPresent()) {
            System.out.println("Episódio encontrado!");
            System.out.println("Temporada: " + episodioBuscado.get().getTemporada());
        }else {
            System.out.println("Episódio não encontrado.");
        }

        //Criando um filtro para ver episódios a partir da data desejada pelo usuário
        System.out.println("A partir de qual ano você deseja ver os episódios?");
        var ano = leitura.nextInt();
        leitura.nextLine();

        LocalDate dataBusca = LocalDate.of(ano, 1, 1);

        DateTimeFormatter formatador = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        episodio.stream()
            .filter(e -> e.getDatalancamento() != null && e.getDatalancamento().isAfter(dataBusca))
            .forEach (e -> System.out.println(
                "Temporada: " + e.getTemporada() + 
                    " Episódio: " + e.getTitulo() +
                    " Data de Lançamento: " + e.getDatalancamento().format(formatador)
            ));

        
        //Criando um mapa com informações das avaliações por temporada
        Map<Integer, Double> avaliacoesPorTemporada = episodio.stream()
            .filter(e -> e.getAvaliacao() > 0.0)
            .collect(Collectors.groupingBy(Episodio::getTemporada,
                Collectors.averagingDouble(Episodio::getAvaliacao)));
            
        System.out.println(avaliacoesPorTemporada);

        DoubleSummaryStatistics est = episodio.stream()
            .filter(e -> e.getAvaliacao() > 0.0)
            .collect(Collectors.summarizingDouble(Episodio::getAvaliacao));

        System.out.println("Média: " + est.getAverage());
        System.out.println("Melhor episódio: " + est.getMax());
        System.out.println("Pior Episódio: " + est.getMin());
        System.out.println("Total de episódios avaliados: " + est. getCount());
    }

}