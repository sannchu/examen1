package es.daw.productojdbc.controller;

import es.daw.productojdbc.dto.FabricanteDto;
import es.daw.productojdbc.exception.FabricanteConProductosException;
import es.daw.productojdbc.exception.FabricanteDuplicadoException;
import es.daw.productojdbc.model.Fabricante;
import es.daw.productojdbc.service.FabricanteService;
import es.daw.productojdbc.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/fabricantes")
@RequiredArgsConstructor
public class FabricanteController {

    private final FabricanteService fabricanteService;
    private final ProductoService productoService;

//    public FabricanteController(FabricanteService fabricanteService) {
//
//        this.fabricanteService = fabricanteService;
//    }

    @GetMapping
    public String listar(Model model) {

        //model.addAttribute("fabricantes", fabricanteService.listar());

        // ------------ RETO 1 --------------
        model.addAttribute("fabricantes",fabricanteService.findAllWithNumProductos());

        return "fabricantes/lista";

    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("fabricanteDto", new FabricanteDto());
        return "fabricantes/formulario";
    }

    @GetMapping("/editar/{codigo}")
    public String editar(@PathVariable Integer codigo, Model model) {
        model.addAttribute("fabricanteDto", FabricanteDto.toDto(fabricanteService.buscarPorCodigo(codigo)));
        return "fabricantes/formulario";
    }

    // ----- RETO 2 ----
    @GetMapping("/{codigo}/productos")
    public String productosDelFabricante(@PathVariable("codigo") Integer codigo, Model model) {

        model.addAttribute("fabricante",fabricanteService.buscarPorCodigo(codigo));
        model.addAttribute("productos",productoService.listarPorFabricante(codigo));

        return "fabricantes/productosDelFabricante";

    }



    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute FabricanteDto fabricanteDto,
                          BindingResult result,
                          RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "fabricantes/formulario";
        }

        try {
            if (fabricanteDto.getCodigo() == null) {
                Fabricante fabricante = fabricanteService.crear(FabricanteDto.toModelEntity(fabricanteDto));
                redirectAttributes.addFlashAttribute("mensaje",
                        "Fabricante creado con código " + fabricante.getCodigo());
            } else {
                Fabricante fabricante = fabricanteService.actualizar(FabricanteDto.toModelEntity(fabricanteDto));
                redirectAttributes.addFlashAttribute("mensaje",
                        "Fabricante " + fabricante.getCodigo() + " actualizado");
            }
        }
        catch (FabricanteDuplicadoException e) {
            result.rejectValue("nombre", "", e.getMessage());
            return "fabricantes/formulario";
        }
        return "redirect:/fabricantes";
    }

    @PostMapping("/borrar/{codigo}")
    public String borrar(@PathVariable("codigo") Integer codigo,
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
