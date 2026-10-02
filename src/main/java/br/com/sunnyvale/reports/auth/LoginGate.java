package br.com.sunnyvale.reports.auth;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class LoginGate {
    public static boolean validar(
            String usuarioDigitado,
            String senhaDigitada,
            Path arquivoLogin
    ) throws Exception {

        Properties p = new Properties();

        try (var r = Files.newBufferedReader(arquivoLogin)) {
            p.load(r);
        }

        String usuarioEsperado = p.getProperty("login.usuario", "");
        String senhaEsperada = p.getProperty("login.senha", "");

        return usuarioDigitado.equals(usuarioEsperado)
                && senhaDigitada.equals(senhaEsperada);
    }
}