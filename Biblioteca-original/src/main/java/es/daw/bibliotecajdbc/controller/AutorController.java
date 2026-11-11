package es.daw.bibliotecajdbc.controller;

import es.daw.bibliotecajdbc.dto.AutorDto;
import es.daw.bibliotecajdbc.mapper.AutorMapper;
import es.daw.bibliotecajdbc.model.Autor;
import es.daw.bibliotecajdbc.service.AutorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/autores")
@RequiredArgsConstructor
public class AutorController {

    private final AutorService autorService;
    private final AutorMapper autorMapper;

    @GetMapping
    public String listar(Model model) {
        List<AutorDto> autores = autorService.listar().stream()
                .map(autorMapper::toDto)
                .toList();
        model.addAttribute("autores", autores);
        return "autores/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("autorDto", new AutorDto());
        return "autores/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        Autor autor = autorService.buscarPorId(id);   // 404 si no existe
        model.addAttribute("autorDto", autorMapper.toDto(autor));
        return "autores/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute AutorDto autorDto,
                          BindingResult result,
                          RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "autores/formulario";
        }

        Autor autor = autorMapper.toModelEntity(autorDto);

        if (autorDto.getId() == null) {
            Autor creado = autorService.crear(autor);
            redirectAttributes.addFlashAttribute("mensaje",
                    "Autor " + creado.getNombre() + " creado");
        } else {
            Autor actualizado = autorService.actualizar(autor);
            redirectAttributes.addFlashAttribute("mensaje",
                    "Autor " + actualizado.getNombre() + " actualizado");
        }

        return "redirect:/autores";
    }

    @PostMapping("/borrar/{id}")
    public String borrar(@PathVariable Integer id,
                         RedirectAttributes redirectAttributes) {
        String nombre = autorService.buscarPorId(id).getNombre();
        int numLibros = autorService.borrar(id);

        String libros = numLibros == 1 ? "1 libro" : numLibros + " libros";
        redirectAttributes.addFlashAttribute("mensaje",
                "Autor " + nombre + " borrado junto con " + libros);

        return "redirect:/autores";
    }
}