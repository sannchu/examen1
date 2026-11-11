package es.daw.productojdbc.service;

import es.daw.productojdbc.exception.FabricanteConProductosException;
import es.daw.productojdbc.exception.RecursoNoEncontradoException;
import es.daw.productojdbc.model.Fabricante;
import es.daw.productojdbc.repository.FabricanteRepository;
import es.daw.productojdbc.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FabricanteService {

    private final FabricanteRepository fabricanteRepository;
    private final ProductoRepository productoRepository;

    public FabricanteService(FabricanteRepository fabricanteRepository,
                             ProductoRepository productoRepository) {
        this.fabricanteRepository = fabricanteRepository;
        this.productoRepository = productoRepository;
    }

    public java.util.List<es.daw.productojdbc.dto.FabricanteResumen> listarResumen() { return fabricanteRepository.findAllConNumeroProductos(); }

    public List<Fabricante> listar() {
        return fabricanteRepository.findAll();
    }

    public Fabricante buscarPorCodigo(Integer codigo) {
        return fabricanteRepository.findById(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el fabricante con código " + codigo));
    }

    public Fabricante crear(Fabricante fabricante) {
        fabricante.setNombre(fabricante.getNombre().trim());
        if (fabricanteRepository.existsByNombreIgnoreCase(fabricante.getNombre())) throw new es.daw.productojdbc.exception.FabricanteDuplicadoException();
        fabricanteRepository.save(fabricante);
        return fabricante;
    }

    public Fabricante actualizar(Fabricante fabricante) {
        buscarPorCodigo(fabricante.getCodigo());
        fabricante.setNombre(fabricante.getNombre().trim());
        if (fabricanteRepository.existsByNombreIgnoreCaseAndCodigoNot(fabricante.getNombre(), fabricante.getCodigo())) throw new es.daw.productojdbc.exception.FabricanteDuplicadoException();
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
