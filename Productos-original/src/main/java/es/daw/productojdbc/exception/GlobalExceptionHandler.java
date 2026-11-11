package es.daw.productojdbc.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Manejo centralizado de errores para TODOS los controladores.
 * Las excepciones que el controlador no captura acaban aquí.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String noEncontrado(RecursoNoEncontradoException ex, Model model) {
        model.addAttribute("titulo", "No encontrado");
        model.addAttribute("mensaje", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(AccesoDatosException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String errorAccesoDatos(AccesoDatosException ex, Model model) {
        // La causa (la SQLException original) va al log, no al usuario
        log.error(ex.getMessage(), ex);
        model.addAttribute("titulo", "Error de base de datos");
        model.addAttribute("mensaje", ex.getMessage());
        return "error";
    }
}