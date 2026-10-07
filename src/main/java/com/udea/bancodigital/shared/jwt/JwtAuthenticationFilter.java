package com.udea.bancodigital.shared.jwt;

import com.udea.bancodigital.usuarios.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String PREFIJO_BEARER = "Bearer ";

    // Marca que deja el filtro cuando el token llego revocado, para que
    // CustomAuthEntryPoint responda TOKEN_REVOKED en lugar de UNAUTHENTICATED.
    public static final String TOKEN_REVOCADO_ATTR = "tokenRevocado";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final RevocacionTokenService revocacionTokenService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UsuarioRepository usuarioRepository,
            RevocacionTokenService revocacionTokenService
    ) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.revocacionTokenService = revocacionTokenService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith(PREFIJO_BEARER)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(PREFIJO_BEARER.length());

        if (!jwtService.esTokenValido(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Un token revocado sigue con firma y fecha validas: no se autentica, y
        // la peticion sigue para que la cadena de seguridad decida. En una ruta
        // protegida eso termina en 401 TOKEN_REVOKED; en una publica, no estorba.
        if (revocacionTokenService.estaRevocado(token)) {
            request.setAttribute(TOKEN_REVOCADO_ATTR, Boolean.TRUE);
            filterChain.doFilter(request, response);
            return;
        }

        String email = jwtService.extraerEmail(token);

        usuarioRepository.findByEmail(email).ifPresent(usuario -> {

            String rol = usuario.getRol().getNombre();

            System.out.println("ROL DEL USUARIO: " + rol);

            var autoridad = new SimpleGrantedAuthority("ROLE_" + rol);

            System.out.println("AUTORIDAD: " + autoridad.getAuthority());

            var autenticacion = new UsernamePasswordAuthenticationToken(
                    usuario.getEmail(),
                    null,
                    List.of(autoridad)
            );

            SecurityContextHolder.getContext().setAuthentication(autenticacion);
        });

        filterChain.doFilter(request, response);
    }
}
