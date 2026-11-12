package es.daw.productojdbc.controller;

import es.daw.productojdbc.dto.ProductoDto;
import es.daw.productojdbc.model.Producto;
import es.daw.productojdbc.service.FabricanteService;
import es.daw.productojdbc.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/productos")
@RequiredArgsConstructor
public class ProductoController {

    // El controlador solo conoce SERVICIOS, nunca repositorios
    private final ProductoService productoService;
    private final FabricanteService fabricanteService;

//    public ProductoController(ProductoService productoService,
//                              FabricanteService fabricanteService) {
//        this.productoService = productoService;
//        this.fabricanteService = fabricanteService;
//    }

    Logger logger = LoggerFactory.getLogger(ProductoController.class);

    // READ: listado
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("productos", productoService.listar());
        return "productos/lista";
    }

    // (aquí irán los métodos de los pasos 12 y 13)

    // Formulario vacío para alta
    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("productoDto", new ProductoDto());
        model.addAttribute("fabricantes", fabricanteService.listar());
        return "productos/formulario";
    }

    // Formulario relleno para edición
    @GetMapping("/editar/{codigo}")
    public String editar(@PathVariable("codigo") Integer codigo, Model model) {

        logger.debug("Editando producto {}", codigo);

        Producto producto = productoService.buscarPorCodigo(codigo);   // 404 si no existe

        model.addAttribute("productoDto", ProductoDto.toDto(producto));
        model.addAttribute("fabricantes", fabricanteService.listar());
        return "productos/formulario";
    }

    // CREATE / UPDATE: el mismo formulario sirve para las dos operaciones
    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute ProductoDto productoDto,
                          BindingResult result,
                          Model model,
                          RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            // Volvemos al formulario: hay que recargar el desplegable
            model.addAttribute("fabricantes", fabricanteService.listar());
            return "productos/formulario";
        }

        if (productoDto.getCodigo() == null) {
            Producto creado = productoService.crear(productoDto.toModelEntity());
            redirectAttributes.addFlashAttribute("mensaje",
                    "Producto creado con código " + creado.getCodigo());
        } else {
            productoService.actualizar(productoDto.toModelEntity());
            redirectAttributes.addFlashAttribute("mensaje",
                    "Producto " + productoDto.getCodigo() + " actualizado");
        }

        // PRG (Post/Redirect/Get): si el usuario pulsa F5 no se reenvía el formulario
        return "redirect:/productos";
    }




    // DELETE: por POST, nunca por GET (un GET no debe modificar datos)
    @PostMapping("/borrar/{codigo}")
    public String borrar(@PathVariable Integer codigo,
                         RedirectAttributes redirectAttributes) {
        productoService.borrar(codigo);
        redirectAttributes.addFlashAttribute("mensaje", "Producto " + codigo + " borrado");
        return "redirect:/productos";
    }


}