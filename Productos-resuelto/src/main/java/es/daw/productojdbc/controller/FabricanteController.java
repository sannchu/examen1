package es.daw.productojdbc.controller;

import es.daw.productojdbc.exception.FabricanteConProductosException;
import es.daw.productojdbc.model.Fabricante;
import es.daw.productojdbc.service.FabricanteService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/fabricantes")
public class FabricanteController {

    private final FabricanteService fabricanteService;
    private final es.daw.productojdbc.service.ProductoService productoService;

    public FabricanteController(FabricanteService fabricanteService, es.daw.productojdbc.service.ProductoService productoService) {
        this.fabricanteService = fabricanteService;
        this.productoService = productoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("fabricantes", fabricanteService.listarResumen());
        return "fabricantes/lista";

    }

    @GetMapping("/{codigo}/productos")
    public String productos(@PathVariable Integer codigo, Model model) {
        model.addAttribute("fabricante", fabricanteService.buscarPorCodigo(codigo));
        model.addAttribute("productos", productoService.listarPorFabricante(codigo));
        return "fabricantes/productos";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("fabricante", new Fabricante());
        return "fabricantes/formulario";
    }

    @GetMapping("/editar/{codigo}")
    public String editar(@PathVariable Integer codigo, Model model) {
        model.addAttribute("fabricante", fabricanteService.buscarPorCodigo(codigo));
        return "fabricantes/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute Fabricante fabricante,
                          BindingResult result,
                          RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "fabricantes/formulario";
        }

        try {
        if (fabricante.getCodigo() == null) {
            fabricanteService.crear(fabricante);
            redirectAttributes.addFlashAttribute("mensaje",
                    "Fabricante creado con código " + fabricante.getCodigo());
        } else {
            fabricanteService.actualizar(fabricante);
            redirectAttributes.addFlashAttribute("mensaje",
                    "Fabricante " + fabricante.getCodigo() + " actualizado");
        }
        } catch (es.daw.productojdbc.exception.FabricanteDuplicadoException e) {
            result.rejectValue("nombre", "duplicado", e.getMessage());
            return "fabricantes/formulario";
        }
        return "redirect:/fabricantes";
    }

    @PostMapping("/borrar/{codigo}")
    public String borrar(@PathVariable Integer codigo,
                         RedirectAttributes redirectAttributes) {
        try {
            fabricanteService.borrar(codigo);
            redirectAttributes.addFlashAttribute("mensaje", "Fabricante " + codigo + " borrado");
        } catch (FabricanteConProductosException e) {
            // Regla de negocio incumplida: avisamos en el listado, no es una página de error
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/fabricantes";
    }


}
