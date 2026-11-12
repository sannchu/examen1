package es.daw.productojdbc.service;

import es.daw.productojdbc.dto.FabricanteResumen;
import es.daw.productojdbc.exception.FabricanteConProductosException;
import es.daw.productojdbc.exception.FabricanteDuplicadoException;
import es.daw.productojdbc.exception.RecursoNoEncontradoException;
import es.daw.productojdbc.model.Fabricante;
import es.daw.productojdbc.repository.FabricanteRepository;
import es.daw.productojdbc.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FabricanteService {

    private final FabricanteRepository fabricanteRepository;
    private final ProductoRepository productoRepository;

//    public FabricanteService(FabricanteRepository fabricanteRepository,
//                             ProductoRepository productoRepository) {
//        this.fabricanteRepository = fabricanteRepository;
//        this.productoRepository = productoRepository;
//    }

    public List<Fabricante> listar() {
        return fabricanteRepository.findAll();
    }

    // ---------------- RETO 1 ----------------
    public List<FabricanteResumen> findAllWithNumProductos() {
        return fabricanteRepository.findAllWithNumProductos();
    }
    // ------------------

    public Fabricante buscarPorCodigo(Integer codigo) {
        return fabricanteRepository.findById(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el fabricante con código " + codigo));
    }

    public Fabricante crear(Fabricante fabricante) {

        // eliminar espacios a izq y derecha
        fabricante.setNombre(fabricante.getNombre().strip());

        // -------------- RETO 3 ------------
        // NUEVA REGLA DE NEGOCIO: nombre único
        // Antes de crear el fabricante, tengo que verificar que no
        // existe ninguno con el mismo nombre
        if (fabricanteRepository.existsByNombreIgnoreCase(fabricante.getNombre())){
            // lanzar excepción propia
            throw new FabricanteDuplicadoException(fabricante.getNombre());
        }

        //----------------------------------------------
        fabricanteRepository.save(fabricante);
        return fabricante;
    }

    public Fabricante actualizar(Fabricante fabricante) {
        // REGLA DE NEGOCIO PARA BUSCAR EL CÓDIGO DE FABRICANTE. SI NO EXISTE LANZA EXCEPCIÓN...
        buscarPorCodigo(fabricante.getCodigo());
        // REGLA DE NEGOCIO PARA ELIMINAR ESPACIOS EN EL NOMBRE DEL FABRICANTE..
        fabricante.setNombre(fabricante.getNombre().strip());

        // -------------- RETO 3 ------------
        // NUEVA REGLA DE NEGOCIO: nombre único
        // Antes de crear el fabricante, tengo que verificar que no
        // existe ninguno con el mismo nombre
        if (fabricanteRepository.existsByNombreIgnoreCaseAndCodigoNot(fabricante.getNombre(),fabricante.getCodigo())){
            // lanzar excepción propia
            throw new FabricanteDuplicadoException(fabricante.getNombre());
        }

        //----------------------------------------------

        fabricanteRepository.update(fabricante);
        return fabricante;
    }

    public void borrar(Integer codigo) {
        buscarPorCodigo(codigo);

        // Regla de negocio: comprobada aquí, antes de llegar a la BD
        if (productoRepository.existsByFabricanteCodigo(codigo)) {
            throw new FabricanteConProductosException(codigo);
        }
        fabricanteRepository.delete(codigo);
    }





}
