package br.com.alura.screenmatch.service;

public interface IConverteDados {
    <T> T obterDados(String jason, Class<T> classe);
}
