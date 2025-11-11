📦 FEATURE SPEC: SISTEMA DE LOCALIZAÇÃO FÍSICA DE ESTOQUE
🎯 CONTEXTO E PROBLEMA
Cenário Real
Oficinas mecânicas gerenciam centenas de peças distribuídas em:

Prateleiras numeradas (ex: "Prateleira 3, Setor A")
Gavetas identificadas (ex: "Gaveta 12-B")
Armários específicos (ex: "Armário de Filtros, Porta 2")
Áreas de armazenamento (ex: "Depósito, Canto Esquerdo")

Pain Point
Tempo perdido procurando peças: Mecânicos e atendentes gastam 5-15 minutos procurando uma peça específica, impactando diretamente:

Tempo de execução de OS
Produtividade da equipe
Satisfação do cliente

Objetivo da Feature
Criar um sistema de localização física inteligente que permita:

✅ Cadastrar localização física de cada peça
✅ Buscar rápida por nome, código ou ID
✅ Visualizar localização exata em segundos
✅ Histórico de movimentações de localização
✅ Alertas de peças "sem localização definida"


🏗️ ARQUITETURA DA SOLUÇÃO
1. Modelo de Dados
   1.1 Estrutura de Localização Hierárquica
   sql-- Tabela: local_armazenamento
   CREATE TABLE local_armazenamento (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   codigo VARCHAR(50) NOT NULL UNIQUE,           -- Ex: "PRAT-03-A"
   tipo VARCHAR(20) NOT NULL,                      -- PRATELEIRA, GAVETA, ARMARIO, DEPOSITO
   descricao VARCHAR(200) NOT NULL,               -- "Prateleira 3, Setor A"
   localizacao_pai_id UUID,                       -- FK para hierarquia (ex: Setor A dentro de Depósito 1)
   capacidade_maxima INTEGER,                     -- Limite de itens
   observacoes TEXT,
   ativo BOOLEAN DEFAULT TRUE,
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

   CONSTRAINT fk_localizacao_pai FOREIGN KEY (localizacao_pai_id)
   REFERENCES local_armazenamento(id)
   );

CREATE INDEX idx_local_codigo ON local_armazenamento(codigo);
CREATE INDEX idx_local_tipo ON local_armazenamento(tipo);
1.2 Relacionamento Peça ↔ Localização
sql-- Extensão da tabela peca existente
ALTER TABLE peca ADD COLUMN local_armazenamento_id UUID;
ALTER TABLE peca ADD CONSTRAINT fk_peca_local
FOREIGN KEY (local_armazenamento_id)
REFERENCES local_armazenamento(id);

CREATE INDEX idx_peca_localizacao ON peca(local_armazenamento_id);
1.3 Histórico de Movimentações
sql-- Tabela: historico_localizacao_peca
CREATE TABLE historico_localizacao_peca (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
peca_id UUID NOT NULL,
local_origem_id UUID,                          -- NULL se primeira localização
local_destino_id UUID NOT NULL,
quantidade_movida INTEGER NOT NULL,
motivo VARCHAR(100),                           -- "Reorganização", "Transferência", etc
usuario_id UUID NOT NULL,
data_movimentacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
observacoes TEXT,

    CONSTRAINT fk_peca FOREIGN KEY (peca_id) REFERENCES peca(id),
    CONSTRAINT fk_local_origem FOREIGN KEY (local_origem_id) REFERENCES local_armazenamento(id),
    CONSTRAINT fk_local_destino FOREIGN KEY (local_destino_id) REFERENCES local_armazenamento(id),
    CONSTRAINT fk_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE INDEX idx_historico_peca ON historico_localizacao_peca(peca_id);
CREATE INDEX idx_historico_data ON historico_localizacao_peca(data_movimentacao DESC);

2. Modelo de Domínio (Backend)
   2.1 Entity: LocalArmazenamento
   javapackage com.pitstop.domain.estoque;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "local_armazenamento")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class LocalArmazenamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String codigo;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoLocal tipo;
    
    @Column(nullable = false, length = 200)
    private String descricao;
    
    // Relacionamento hierárquico (ex: Gaveta dentro de Armário)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "localizacao_pai_id")
    private LocalArmazenamento localizacaoPai;
    
    @OneToMany(mappedBy = "localizacaoPai", cascade = CascadeType.ALL)
    private Set<LocalArmazenamento> locaisFilhos = new HashSet<>();
    
    @Column(name = "capacidade_maxima")
    private Integer capacidadeMaxima;
    
    @Column(columnDefinition = "TEXT")
    private String observacoes;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Retorna o caminho completo da localização hierárquica.
     * Ex: "Depósito 1 > Setor B > Prateleira 5"
     */
    public String getCaminhoCompleto() {
        if (localizacaoPai == null) {
            return descricao;
        }
        return localizacaoPai.getCaminhoCompleto() + " > " + descricao;
    }
    
    /**
     * Verifica se o local está no limite de capacidade.
     */
    public boolean isCapacidadeCheia(long quantidadeAtual) {
        return capacidadeMaxima != null && quantidadeAtual >= capacidadeMaxima;
    }
}

// Enum de tipos de localização
public enum TipoLocal {
PRATELEIRA("Prateleira"),
GAVETA("Gaveta"),
ARMARIO("Armário"),
DEPOSITO("Depósito"),
CAIXA("Caixa Organizadora"),
VITRINE("Vitrine"),
OUTRO("Outro");

    private final String descricao;
    
    TipoLocal(String descricao) {
        this.descricao = descricao;
    }
    
    public String getDescricao() {
        return descricao;
    }
}
2.2 Service: LocalizacaoEstoqueService
javapackage com.pitstop.service.estoque;

import com.pitstop.domain.estoque.*;
import com.pitstop.dto.estoque.*;
import com.pitstop.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocalizacaoEstoqueService {

    private final LocalArmazenamentoRepository localRepository;
    private final PecaRepository pecaRepository;
    private final HistoricoLocalizacaoPecaRepository historicoRepository;
    
    /**
     * Busca peças por múltiplos critérios com localização.
     * 
     * @param searchTerm Termo de busca (nome, código, ID)
     * @return Lista de peças com suas localizações
     */
    @Transactional(readOnly = true)
    public List<PecaComLocalizacaoDTO> buscarPecasComLocalizacao(String searchTerm) {
        log.info("Buscando peças com localização. Termo: {}", searchTerm);
        
        // Busca inteligente: código OU nome OU ID
        List<Peca> pecas = pecaRepository.findByCodigoOrDescricaoContainingIgnoreCase(
            searchTerm, searchTerm
        );
        
        return pecas.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Atribui ou move uma peça para uma nova localização.
     * 
     * @param pecaId ID da peça
     * @param novoLocalId ID do novo local
     * @param quantidade Quantidade a mover (para controle parcial)
     * @param motivo Motivo da movimentação
     * @param usuarioId Usuário responsável
     */
    @Transactional
    public void moverPecaParaLocal(
        UUID pecaId, 
        UUID novoLocalId, 
        Integer quantidade,
        String motivo,
        UUID usuarioId
    ) {
        Peca peca = pecaRepository.findById(pecaId)
            .orElseThrow(() -> new ResourceNotFoundException("Peça não encontrada"));
        
        LocalArmazenamento novoLocal = localRepository.findById(novoLocalId)
            .orElseThrow(() -> new ResourceNotFoundException("Local não encontrado"));
        
        // Verifica capacidade do local
        long pecasNoLocal = pecaRepository.countByLocalArmazenamentoId(novoLocalId);
        if (novoLocal.isCapacidadeCheia(pecasNoLocal)) {
            throw new BusinessException("Local está na capacidade máxima");
        }
        
        // Registra histórico
        HistoricoLocalizacaoPeca historico = HistoricoLocalizacaoPeca.builder()
            .pecaId(pecaId)
            .localOrigemId(peca.getLocalArmazenamento() != null 
                ? peca.getLocalArmazenamento().getId() 
                : null)
            .localDestinoId(novoLocalId)
            .quantidadeMovida(quantidade != null ? quantidade : peca.getQuantidadeAtual())
            .motivo(motivo)
            .usuarioId(usuarioId)
            .build();
        
        historicoRepository.save(historico);
        
        // Atualiza localização da peça
        peca.setLocalArmazenamento(novoLocal);
        pecaRepository.save(peca);
        
        log.info("Peça {} movida para local {}. Motivo: {}", 
            peca.getCodigo(), novoLocal.getCodigo(), motivo);
    }
    
    /**
     * Lista todas as peças em um local específico.
     */
    @Transactional(readOnly = true)
    public List<PecaComLocalizacaoDTO> listarPecasPorLocal(UUID localId) {
        List<Peca> pecas = pecaRepository.findByLocalArmazenamentoId(localId);
        return pecas.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Identifica peças sem localização definida.
     */
    @Transactional(readOnly = true)
    public List<PecaSemLocalizacaoDTO> listarPecasSemLocalizacao() {
        List<Peca> pecas = pecaRepository.findByLocalArmazenamentoIsNull();
        
        return pecas.stream()
            .map(peca -> PecaSemLocalizacaoDTO.builder()
                .pecaId(peca.getId())
                .codigo(peca.getCodigo())
                .descricao(peca.getDescricao())
                .quantidadeAtual(peca.getQuantidadeAtual())
                .build())
            .collect(Collectors.toList());
    }
    
    /**
     * Gera sugestão de localização baseada em critérios.
     */
    @Transactional(readOnly = true)
    public LocalArmazenamento sugerirLocalizacao(Peca peca) {
        // Lógica de sugestão:
        // 1. Peças similares (mesma categoria/aplicação)
        // 2. Locais com espaço disponível
        // 3. Frequência de uso (peças de giro rápido em locais acessíveis)
        
        // Implementação simplificada: retorna local com mais espaço
        return localRepository.findTopByAtivoTrueOrderByCapacidadeDisponivel()
            .orElse(null);
    }
    
    private PecaComLocalizacaoDTO mapToDTO(Peca peca) {
        LocalArmazenamento local = peca.getLocalArmazenamento();
        
        return PecaComLocalizacaoDTO.builder()
            .pecaId(peca.getId())
            .codigo(peca.getCodigo())
            .descricao(peca.getDescricao())
            .quantidadeAtual(peca.getQuantidadeAtual())
            .localizacao(local != null ? LocalizacaoDTO.builder()
                .localId(local.getId())
                .codigo(local.getCodigo())
                .tipo(local.getTipo())
                .descricao(local.getDescricao())
                .caminhoCompleto(local.getCaminhoCompleto())
                .build() : null)
            .temLocalizacao(local != null)
            .build();
    }
}
2.3 Controller: LocalizacaoEstoqueController
javapackage com.pitstop.controller;

import com.pitstop.dto.estoque.*;
import com.pitstop.service.estoque.LocalizacaoEstoqueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/estoque/localizacao")
@Tag(name = "Localização de Estoque", description = "Gerenciamento de localização física de peças")
@RequiredArgsConstructor
public class LocalizacaoEstoqueController {

    private final LocalizacaoEstoqueService localizacaoService;
    
    @GetMapping("/buscar")
    @Operation(summary = "Buscar peças por nome, código ou ID com localização")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    public ResponseEntity<List<PecaComLocalizacaoDTO>> buscarPecas(
        @RequestParam String termo
    ) {
        return ResponseEntity.ok(
            localizacaoService.buscarPecasComLocalizacao(termo)
        );
    }
    
    @PostMapping("/mover")
    @Operation(summary = "Mover peça para nova localização")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    public ResponseEntity<Void> moverPeca(
        @RequestBody MoverPecaRequest request,
        @RequestParam UUID usuarioId
    ) {
        localizacaoService.moverPecaParaLocal(
            request.getPecaId(),
            request.getNovoLocalId(),
            request.getQuantidade(),
            request.getMotivo(),
            usuarioId
        );
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/sem-localizacao")
    @Operation(summary = "Listar peças sem localização definida")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<List<PecaSemLocalizacaoDTO>> listarSemLocalizacao() {
        return ResponseEntity.ok(
            localizacaoService.listarPecasSemLocalizacao()
        );
    }
    
    @GetMapping("/local/{localId}/pecas")
    @Operation(summary = "Listar todas as peças em um local específico")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    public ResponseEntity<List<PecaComLocalizacaoDTO>> listarPecasPorLocal(
        @PathVariable UUID localId
    ) {
        return ResponseEntity.ok(
            localizacaoService.listarPecasPorLocal(localId)
        );
    }
}

3. Frontend Implementation
   3.1 Types & Interfaces
   typescript// types/estoque.ts

export enum TipoLocal {
PRATELEIRA = 'PRATELEIRA',
GAVETA = 'GAVETA',
ARMARIO = 'ARMARIO',
DEPOSITO = 'DEPOSITO',
CAIXA = 'CAIXA',
VITRINE = 'VITRINE',
OUTRO = 'OUTRO'
}

export interface LocalizacaoDTO {
localId: string;
codigo: string;
tipo: TipoLocal;
descricao: string;
caminhoCompleto: string; // "Depósito 1 > Setor A > Prateleira 3"
}

export interface PecaComLocalizacaoDTO {
pecaId: string;
codigo: string;
descricao: string;
quantidadeAtual: number;
localizacao: LocalizacaoDTO | null;
temLocalizacao: boolean;
}

export interface LocalArmazenamento {
id: string;
codigo: string;
tipo: TipoLocal;
descricao: string;
localizacaoPai?: LocalArmazenamento;
capacidadeMaxima?: number;
observacoes?: string;
ativo: boolean;
}
3.2 Component: BuscaRapidaPeca
tsx// components/estoque/BuscaRapidaPeca.tsx

import { useState } from 'react';
import { Search, MapPin, Package, AlertCircle } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { Input } from '@/components/ui/input';
import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import { buscarPecasComLocalizacao } from '@/services/estoque-api';
import type { PecaComLocalizacaoDTO } from '@/types/estoque';

export function BuscaRapidaPeca() {
const [searchTerm, setSearchTerm] = useState('');
const [debouncedTerm, setDebouncedTerm] = useState('');

// Debounce para evitar requisições excessivas
useEffect(() => {
const timer = setTimeout(() => {
setDebouncedTerm(searchTerm);
}, 300);
return () => clearTimeout(timer);
}, [searchTerm]);

const { data: pecas, isLoading } = useQuery({
queryKey: ['pecas-localizacao', debouncedTerm],
queryFn: () => buscarPecasComLocalizacao(debouncedTerm),
enabled: debouncedTerm.length >= 3,
staleTime: 30000 // 30 segundos
});

return (
<div className="w-full max-w-2xl space-y-4">
{/* Campo de busca */}
<div className="relative">
<Search className="absolute left-3 top-3 h-5 w-5 text-muted-foreground" />
<Input
type="text"
placeholder="Busque por nome, código ou ID da peça..."
value={searchTerm}
onChange={(e) => setSearchTerm(e.target.value)}
className="pl-10 pr-4 h-12 text-lg"
autoFocus
/>
</div>

      {/* Resultados */}
      {isLoading && (
        <div className="text-center py-8 text-muted-foreground">
          Buscando peças...
        </div>
      )}

      {pecas && pecas.length === 0 && (
        <Card className="p-6 text-center text-muted-foreground">
          <Package className="h-12 w-12 mx-auto mb-3 opacity-50" />
          <p>Nenhuma peça encontrada</p>
        </Card>
      )}

      {pecas && pecas.length > 0 && (
        <div className="space-y-2">
          {pecas.map((peca) => (
            <PecaResultCard key={peca.pecaId} peca={peca} />
          ))}
        </div>
      )}
    </div>
);
}

// Componente de card de resultado
function PecaResultCard({ peca }: { peca: PecaComLocalizacaoDTO }) {
const hasLocation = peca.temLocalizacao && peca.localizacao;

return (
<Card className={cn(
"p-4 transition-all hover:shadow-md cursor-pointer",
!hasLocation && "border-amber-500/50 bg-amber-50/30"
)}>
<div className="flex items-start justify-between">
<div className="flex-1 space-y-2">
{/* Código e nome da peça */}
<div className="flex items-center gap-3">
<Badge variant="outline" className="font-mono">
{peca.codigo}
</Badge>
<h3 className="font-semibold text-lg">{peca.descricao}</h3>
</div>

          {/* Quantidade */}
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Package className="h-4 w-4" />
            <span>{peca.quantidadeAtual} unidades em estoque</span>
          </div>

          {/* Localização */}
          {hasLocation ? (
            <div className="flex items-start gap-2 p-3 bg-green-50 border border-green-200 rounded-lg">
              <MapPin className="h-5 w-5 text-green-600 mt-0.5 flex-shrink-0" />
              <div className="flex-1">
                <p className="text-sm font-medium text-green-900">
                  Localização
                </p>
                <p className="text-sm text-green-700">
                  {peca.localizacao.caminhoCompleto}
                </p>
                <p className="text-xs text-green-600 mt-1">
                  Código: <span className="font-mono">{peca.localizacao.codigo}</span>
                </p>
              </div>
            </div>
          ) : (
            <div className="flex items-start gap-2 p-3 bg-amber-50 border border-amber-200 rounded-lg">
              <AlertCircle className="h-5 w-5 text-amber-600 mt-0.5" />
              <div>
                <p className="text-sm font-medium text-amber-900">
                  Localização não definida
                </p>
                <button className="text-xs text-amber-700 underline mt-1 hover:text-amber-900">
                  Definir localização
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </Card>
);
}
3.3 Component: GerenciadorLocalizacoes
tsx// components/estoque/GerenciadorLocalizacoes.tsx

import { useState } from 'react';
import { Plus, Edit, Trash2, FolderTree } from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import {
Dialog,
DialogContent,
DialogHeader,
DialogTitle,
DialogTrigger
} from '@/components/ui/dialog';
import { FormularioLocal } from './FormularioLocal';
import { listarLocaisArmazenamento } from '@/services/estoque-api';
import type { LocalArmazenamento } from '@/types/estoque';

export function GerenciadorLocalizacoes() {
const [localSelecionado, setLocalSelecionado] = useState<LocalArmazenamento | null>(null);
const queryClient = useQueryClient();

const { data: locais, isLoading } = useQuery({
queryKey: ['locais-armazenamento'],
queryFn: listarLocaisArmazenamento
});

// Organiza locais em hierarquia
const locaisHierarquicos = organizarHierarquia(locais || []);

return (
<div className="space-y-6">
{/* Header */}
<div className="flex items-center justify-between">
<div>
<h2 className="text-2xl font-bold">Locais de Armazenamento</h2>
<p className="text-muted-foreground">
Gerencie prateleiras, gavetas e locais de estoque
</p>
</div>

        <Dialog>
          <DialogTrigger asChild>
            <Button>
              <Plus className="h-4 w-4 mr-2" />
              Novo Local
            </Button>
          </DialogTrigger>
          <DialogContent className="max-w-2xl">
            <DialogHeader>
              <DialogTitle>Cadastrar Local de Armazenamento</DialogTitle>
            </DialogHeader>
            <FormularioLocal onSuccess={() => {
              queryClient.invalidateQueries(['locais-armazenamento']);
            }} />
          </DialogContent>
        </Dialog>
      </div>

      {/* Lista hierárquica */}
      {isLoading ? (
        <div className="text-center py-12">Carregando locais...</div>
      ) : (
        <div className="space-y-2">
          {locaisHierarquicos.map((local) => (
            <LocalCard 
              key={local.id} 
              local={local}
              nivel={0}
            />
          ))}
        </div>
      )}
    </div>
);
}

// Componente recursivo para hierarquia
function LocalCard({
local,
nivel
}: {
local: LocalArmazenamento & { filhos?: LocalArmazenamento[] };
nivel: number;
}) {
const [expandido, setExpandido] = useState(false);
const hasFilhos = local.filhos && local.filhos.length > 0;

return (
<div className={cn("space-y-2", nivel > 0 && "ml-6 border-l-2 border-gray-200 pl-4")}>
<Card className="p-4">
<div className="flex items-center justify-between">
<div className="flex items-center gap-3 flex-1">
{hasFilhos && (
<button
onClick={() => setExpandido(!expandido)}
className="text-muted-foreground hover:text-foreground"
>
<FolderTree className={cn(
"h-5 w-5 transition-transform",
expandido && "rotate-90"
)} />
</button>
)}

            <div className="flex-1">
              <div className="flex items-center gap-2">
                <Badge variant="outline" className="font-mono">
                  {local.codigo}
                </Badge>
                <Badge variant="secondary">
                  {local.tipo}
                </Badge>
              </div>
              <p className="font-medium mt-1">{local.descricao}</p>
              {local.capacidadeMaxima && (
                <p className="text-sm text-muted-foreground">
                  Capacidade: {local.capacidadeMaxima} unidades
                </p>
              )}
            </div>
          </div>

          <div className="flex items-center gap-2">
            <Button variant="ghost" size="sm">
              <Edit className="h-4 w-4" />
            </Button>
            <Button variant="ghost" size="sm">
              <Trash2 className="h-4 w-4 text-destructive" />
            </Button>
          </div>
        </div>
      </Card>

      {/* Filhos (recursivo) */}
      {expandido && hasFilhos && (
        <div className="space-y-2">
          {local.filhos!.map((filho) => (
            <LocalCard 
              key={filho.id} 
              local={filho} 
              nivel={nivel + 1} 
            />
          ))}
        </div>
      )}
    </div>
);
}

// Função auxiliar para organizar hierarquia
function organizarHierarquia(locais: LocalArmazenamento[]) {
const mapa = new Map<string, LocalArmazenamento & { filhos?: LocalArmazenamento[] }>();
const raiz: (LocalArmazenamento & { filhos?: LocalArmazenamento[] })[] = [];

// Primeiro, criar mapa de todos os locais
locais.forEach(local => {
mapa.set(local.id, { ...local, filhos: [] });
});

// Depois, organizar hierarquia
locais.forEach(local => {
const node = mapa.get(local.id)!;
if (local.localizacaoPai) {
const pai = mapa.get(local.localizacaoPai.id);
if (pai) {
pai.filhos!.push(node);
}
} else {
raiz.push(node);
}
});

return raiz;
}

🎯 FLUXOS DE USO
Fluxo 1: Busca Rápida (Caso Principal)
mermaidsequenceDiagram
actor Usuario as Usuário
participant UI as Interface
participant API as Backend API
participant DB as Database

    Usuario->>UI: Digite "filtro de óleo" na busca
    UI->>UI: Debounce 300ms
    UI->>API: GET /api/estoque/localizacao/buscar?termo=filtro
    API->>DB: Query peças + joins localização
    DB-->>API: Lista de peças com localização
    API-->>UI: Response JSON
    UI->>UI: Renderiza cards com localização
    UI-->>Usuario: Mostra "Prateleira 3-A, Gaveta 5"
Fluxo 2: Definir/Mover Localização
mermaidsequenceDiagram
actor Usuario as Atendente
participant UI as Interface
participant API as Backend API
participant DB as Database

    Usuario->>UI: Clica em "Definir localização" na peça
    UI->>UI: Abre modal com seletor de locais
    Usuario->>UI: Seleciona "Prateleira 7-B"
    UI->>API: POST /api/estoque/localizacao/mover
    API->>DB: UPDATE peca SET local_id
    API->>DB: INSERT historico_localizacao
    DB-->>API: Success
    API-->>UI: 200 OK
    UI->>Usuario: Toast: "Localização atualizada!"

📊 MÉTRICAS E BENEFÍCIOS
KPIs de Sucesso
MétricaAntesDepoisMelhoriaTempo médio de busca de peça8-15 min30 seg94% ↓Peças sem localizaçãoN/A< 5%MonitorávelErros de separação12%< 3%75% ↓Produtividade mecânicoBaseline+20%Medido
ROI Estimado

Tempo economizado: 10-14 min/dia por mecânico
2 mecânicos: ~40 horas/mês recuperadas
Valor: R$ 2.000-3.000/mês em produtividade


🚀 ROADMAP DE IMPLEMENTAÇÃO
Fase 1: MVP (Sprint 1-2 semanas)

✅ Modelo de dados (migrations)
✅ CRUD básico de locais
✅ Busca de peças com localização
✅ Interface de busca rápida

Fase 2: Melhorias (Sprint 2-3 semanas)

📱 App mobile para leitura QR Code/NFC
🏷️ Geração de etiquetas para impressão
📊 Dashboard de ocupação de locais
🔔 Alertas de peças sem localização

Fase 3: Avançado (Futuro)

🤖 IA para sugestão inteligente de localização
📸 Fotos dos locais físicos
🗺️ Mapa visual do layout da oficina
📈 Analytics de movimentações


⚠️ CONSIDERAÇÕES TÉCNICAS
Performance

Índices críticos: peca.local_armazenamento_id, historico_localizacao_peca(peca_id, data)
Cache: Locais de armazenamento (raramente mudam) → Redis, TTL 1 hora
Debounce frontend: 300ms para evitar chamadas excessivas

Escalabilidade

Multi-tenant ready: FK oficina_id em local_armazenamento
Suporta hierarquias profundas: Estrutura auto-referencial
Histórico ilimitado: Particionamento por data futuramente

UX Critical

⚡ Busca deve ser instantânea (< 500ms)
🎯 Destaque visual para peças sem localização
📱 Interface mobile-first (mecânicos usam celular)


🎓 JUSTIFICATIVAS ARQUITETURAIS
Por que hierarquia de locais?
Realidade das oficinas: "Está na gaveta 3 do armário 2 do depósito principal"

Flexibilidade para diferentes layouts
Facilita reorganizações físicas
Suporta sub-localizações (caixas dentro de prateleiras)

Por que histórico de movimentações?

Auditoria: "Quem moveu esta peça?"
Analytics: Identificar peças que mudam muito de lugar
Troubleshooting: Rastrear reorganizações

Por que campo "caminhoCompleto"?

UX: Usuário vê caminho completo sem precisar navegar
Performance: Pré-computado no backend (1 query vs N queries)
Consistency: Calculado no método do entity


🎯 Resultado Final: Sistema profissional de localização que transforma gestão de estoque de "caótica" para "organizada", economizando horas de trabalho diariamente e reduzindo erros drasticamente.Tentar novamenteJTe se o cliente quiser adicionar prateleiras, gavetas etc?Architected comprehensive CRUD system for dynamic storage location management.O usuário está perguntando sobre a funcionalidade de CRUD (Create, Read, Update, Delete) para os locais de armazenamento - ou seja, permitir que o cliente adicione, edite e remova prateleiras, gavetas etc dinamicamente pelo sistema.
Essa é uma questão essencial que preciso expandir na documentação. Vou criar uma seção completa mostrando:

Interface de gerenciamento de locais
Formulários para CRUD
Validações e regras de negócio
Considerações de UX
Migração de peças quando local é removido
Bulk operations
Importação/exportação de layouts

Vou seguir o estilo técnico detalhado do usuário, com justificativas arquiteturais e código completo.🏗️ MÓDULO COMPLETO: GERENCIAMENTO DINÂMICO DE LOCAIS
🎯 REQUISITO FUNCIONAL
User Story: "Como gestor da oficina, preciso cadastrar, editar e organizar minhas prateleiras, gavetas e locais de armazenamento conforme meu layout físico muda"
Casos de Uso

✅ Criar novos locais (prateleiras, gavetas, armários)
✅ Editar informações de locais existentes
✅ Inativar/Reativar locais (soft delete)
✅ Excluir locais vazios
✅ Reorganizar hierarquia (mover gaveta para outro armário)
✅ Importar layout em massa (CSV/Excel)
✅ Gerar etiquetas para impressão


🗄️ BACKEND: CRUD COMPLETO
1. Repository Layer
   javapackage com.pitstop.repository;

import com.pitstop.domain.estoque.LocalArmazenamento;
import com.pitstop.domain.estoque.TipoLocal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface LocalArmazenamentoRepository extends JpaRepository<LocalArmazenamento, UUID> {

    /**
     * Busca por código exato (único).
     */
    Optional<LocalArmazenamento> findByCodigo(String codigo);
    
    /**
     * Verifica se código já existe (para validação unique).
     */
    boolean existsByCodigo(String codigo);
    
    /**
     * Lista apenas locais ativos.
     */
    List<LocalArmazenamento> findByAtivoTrue();
    
    /**
     * Lista locais raiz (sem pai) - primeiro nível da hierarquia.
     */
    @Query("SELECT l FROM LocalArmazenamento l WHERE l.localizacaoPai IS NULL AND l.ativo = true")
    List<LocalArmazenamento> findLocaisRaiz();
    
    /**
     * Lista filhos de um local específico.
     */
    List<LocalArmazenamento> findByLocalizacaoPaiId(UUID paiId);
    
    /**
     * Busca locais por tipo.
     */
    List<LocalArmazenamento> findByTipoAndAtivoTrue(TipoLocal tipo);
    
    /**
     * Busca com filtros múltiplos e paginação.
     */
    @Query("SELECT l FROM LocalArmazenamento l WHERE " +
           "(:tipo IS NULL OR l.tipo = :tipo) AND " +
           "(:ativo IS NULL OR l.ativo = :ativo) AND " +
           "(:searchTerm IS NULL OR LOWER(l.descricao) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "   OR LOWER(l.codigo) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<LocalArmazenamento> findWithFilters(
        @Param("tipo") TipoLocal tipo,
        @Param("ativo") Boolean ativo,
        @Param("searchTerm") String searchTerm,
        Pageable pageable
    );
    
    /**
     * Conta peças vinculadas a um local.
     */
    @Query("SELECT COUNT(p) FROM Peca p WHERE p.localArmazenamento.id = :localId")
    long countPecasVinculadas(@Param("localId") UUID localId);
    
    /**
     * Encontra local com mais espaço disponível (para sugestões).
     */
    @Query("SELECT l FROM LocalArmazenamento l WHERE l.ativo = true " +
           "AND l.capacidadeMaxima IS NOT NULL " +
           "ORDER BY (l.capacidadeMaxima - " +
           "   (SELECT COUNT(p) FROM Peca p WHERE p.localArmazenamento.id = l.id)) DESC")
    Optional<LocalArmazenamento> findTopByAtivoTrueOrderByCapacidadeDisponivel();
    
    /**
     * Valida se há ciclo na hierarquia (prevenir: A -> B -> A).
     */
    @Query(value = """
        WITH RECURSIVE hierarquia AS (
            SELECT id, localizacao_pai_id, 1 as nivel
            FROM local_armazenamento
            WHERE id = :localId
            
            UNION ALL
            
            SELECT l.id, l.localizacao_pai_id, h.nivel + 1
            FROM local_armazenamento l
            INNER JOIN hierarquia h ON l.id = h.localizacao_pai_id
            WHERE h.nivel < 10
        )
        SELECT COUNT(*) > 0 
        FROM hierarquia 
        WHERE localizacao_pai_id = :novoPaiId
        """, nativeQuery = true)
    boolean verificaCicloHierarquia(
        @Param("localId") UUID localId, 
        @Param("novoPaiId") UUID novoPaiId
    );
}

2. DTOs de Request e Response
   javapackage com.pitstop.dto.estoque;

import com.pitstop.domain.estoque.TipoLocal;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.UUID;

/**
* DTO para criação de novo local de armazenamento.
  */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public class CriarLocalRequest {

  @NotBlank(message = "Código é obrigatório")
  @Size(max = 50, message = "Código deve ter no máximo 50 caracteres")
  @Pattern(
  regexp = "^[A-Z0-9-]+$",
  message = "Código deve conter apenas letras maiúsculas, números e hífen"
  )
  private String codigo;

  @NotNull(message = "Tipo é obrigatório")
  private TipoLocal tipo;

  @NotBlank(message = "Descrição é obrigatória")
  @Size(max = 200, message = "Descrição deve ter no máximo 200 caracteres")
  private String descricao;

  /**
    * ID do local pai (opcional - se null, é um local raiz).
      */
      private UUID localizacaoPaiId;

  @Min(value = 1, message = "Capacidade deve ser no mínimo 1")
  @Max(value = 10000, message = "Capacidade máxima permitida: 10.000")
  private Integer capacidadeMaxima;

  @Size(max = 500, message = "Observações devem ter no máximo 500 caracteres")
  private String observacoes;
  }

/**
* DTO para atualização de local existente.
  */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public class AtualizarLocalRequest {

  @NotBlank(message = "Código é obrigatório")
  @Size(max = 50)
  private String codigo;

  @NotNull(message = "Tipo é obrigatório")
  private TipoLocal tipo;

  @NotBlank(message = "Descrição é obrigatória")
  @Size(max = 200)
  private String descricao;

  private UUID localizacaoPaiId;

  @Min(1)
  private Integer capacidadeMaxima;

  @Size(max = 500)
  private String observacoes;

  @NotNull(message = "Status ativo é obrigatório")
  private Boolean ativo;
  }

/**
* DTO de resposta completo.
  */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public class LocalArmazenamentoDTO {
  private UUID id;
  private String codigo;
  private TipoLocal tipo;
  private String descricao;
  private LocalArmazenamentoSimplificadoDTO localizacaoPai;
  private Integer capacidadeMaxima;
  private Long capacidadeUtilizada;
  private Long capacidadeDisponivel;
  private String observacoes;
  private Boolean ativo;
  private String caminhoCompleto;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  /**
    * Flag indicando se pode ser excluído (sem peças vinculadas).
      */
      private Boolean podeExcluir;

  /**
    * Estatísticas do local.
      */
      private LocalEstatisticasDTO estatisticas;
      }

/**
* DTO simplificado para referências (evitar recursão infinita).
  */
  @Data
  @Builder
  public class LocalArmazenamentoSimplificadoDTO {
  private UUID id;
  private String codigo;
  private String descricao;
  }

/**
* Estatísticas de uso do local.
  */
  @Data
  @Builder
  public class LocalEstatisticasDTO {
  private Long totalPecas;
  private Long pecasDiferentes;
  private Double valorTotalEstoque;
  private Double percentualOcupacao; // capacidadeUtilizada / capacidadeMaxima * 100
  }

3. Service Layer: CRUD Completo
   javapackage com.pitstop.service.estoque;

import com.pitstop.domain.estoque.LocalArmazenamento;
import com.pitstop.dto.estoque.*;
import com.pitstop.exception.*;
import com.pitstop.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocalArmazenamentoService {

    private final LocalArmazenamentoRepository localRepository;
    private final PecaRepository pecaRepository;
    
    /**
     * Lista todos os locais com filtros e paginação.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "locais-cache", key = "#tipo + '_' + #ativo + '_' + #searchTerm + '_' + #pageable")
    public Page<LocalArmazenamentoDTO> listarLocais(
        TipoLocal tipo,
        Boolean ativo,
        String searchTerm,
        Pageable pageable
    ) {
        log.info("Listando locais. Filtros - Tipo: {}, Ativo: {}, Busca: {}", 
            tipo, ativo, searchTerm);
        
        Page<LocalArmazenamento> locais = localRepository.findWithFilters(
            tipo, ativo, searchTerm, pageable
        );
        
        return locais.map(this::mapToDTO);
    }
    
    /**
     * Busca local por ID.
     */
    @Transactional(readOnly = true)
    public LocalArmazenamentoDTO buscarPorId(UUID id) {
        LocalArmazenamento local = localRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Local de armazenamento não encontrado"
            ));
        
        return mapToDTO(local);
    }
    
    /**
     * Cria novo local de armazenamento.
     */
    @Transactional
    @CacheEvict(value = "locais-cache", allEntries = true)
    public LocalArmazenamentoDTO criarLocal(CriarLocalRequest request) {
        log.info("Criando novo local. Código: {}, Tipo: {}", 
            request.getCodigo(), request.getTipo());
        
        // Validação 1: Código único
        if (localRepository.existsByCodigo(request.getCodigo())) {
            throw new BusinessException("Já existe um local com o código: " + request.getCodigo());
        }
        
        // Validação 2: Pai existe (se especificado)
        LocalArmazenamento pai = null;
        if (request.getLocalizacaoPaiId() != null) {
            pai = localRepository.findById(request.getLocalizacaoPaiId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Local pai não encontrado"
                ));
            
            if (!pai.getAtivo()) {
                throw new BusinessException(
                    "Não é possível criar local dentro de um local inativo"
                );
            }
        }
        
        // Validação 3: Lógica de tipos hierárquicos (regras de negócio)
        validarHierarquiaTipos(request.getTipo(), pai);
        
        // Criação
        LocalArmazenamento novoLocal = LocalArmazenamento.builder()
            .codigo(request.getCodigo().toUpperCase()) // Normalizar para maiúsculas
            .tipo(request.getTipo())
            .descricao(request.getDescricao())
            .localizacaoPai(pai)
            .capacidadeMaxima(request.getCapacidadeMaxima())
            .observacoes(request.getObservacoes())
            .ativo(true)
            .build();
        
        LocalArmazenamento salvo = localRepository.save(novoLocal);
        
        log.info("Local criado com sucesso. ID: {}, Código: {}", 
            salvo.getId(), salvo.getCodigo());
        
        return mapToDTO(salvo);
    }
    
    /**
     * Atualiza local existente.
     */
    @Transactional
    @CacheEvict(value = "locais-cache", allEntries = true)
    public LocalArmazenamentoDTO atualizarLocal(UUID id, AtualizarLocalRequest request) {
        log.info("Atualizando local {}. Novo código: {}", id, request.getCodigo());
        
        LocalArmazenamento local = localRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Local não encontrado"));
        
        // Validação 1: Código único (se mudou)
        if (!local.getCodigo().equals(request.getCodigo())) {
            if (localRepository.existsByCodigo(request.getCodigo())) {
                throw new BusinessException(
                    "Já existe outro local com o código: " + request.getCodigo()
                );
            }
        }
        
        // Validação 2: Novo pai (se mudou)
        LocalArmazenamento novoPai = null;
        if (request.getLocalizacaoPaiId() != null) {
            novoPai = localRepository.findById(request.getLocalizacaoPaiId())
                .orElseThrow(() -> new ResourceNotFoundException("Local pai não encontrado"));
            
            // Prevenir ciclo: A -> B -> A
            if (localRepository.verificaCicloHierarquia(id, novoPai.getId())) {
                throw new BusinessException(
                    "Não é possível mover: criaria um ciclo na hierarquia"
                );
            }
        }
        
        // Validação 3: Inativar local com peças
        if (!request.getAtivo() && local.getAtivo()) {
            long pecasVinculadas = localRepository.countPecasVinculadas(id);
            if (pecasVinculadas > 0) {
                throw new BusinessException(
                    String.format(
                        "Não é possível inativar local com %d peças vinculadas. " +
                        "Mova as peças primeiro.",
                        pecasVinculadas
                    )
                );
            }
        }
        
        // Atualização
        local.setCodigo(request.getCodigo().toUpperCase());
        local.setTipo(request.getTipo());
        local.setDescricao(request.getDescricao());
        local.setLocalizacaoPai(novoPai);
        local.setCapacidadeMaxima(request.getCapacidadeMaxima());
        local.setObservacoes(request.getObservacoes());
        local.setAtivo(request.getAtivo());
        
        LocalArmazenamento atualizado = localRepository.save(local);
        
        log.info("Local {} atualizado com sucesso", id);
        
        return mapToDTO(atualizado);
    }
    
    /**
     * Exclui local (hard delete).
     * Só permite se não houver peças vinculadas E não houver locais filhos.
     */
    @Transactional
    @CacheEvict(value = "locais-cache", allEntries = true)
    public void excluirLocal(UUID id) {
        log.warn("Tentativa de exclusão do local {}", id);
        
        LocalArmazenamento local = localRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Local não encontrado"));
        
        // Validação 1: Sem peças vinculadas
        long pecasVinculadas = localRepository.countPecasVinculadas(id);
        if (pecasVinculadas > 0) {
            throw new BusinessException(
                String.format(
                    "Não é possível excluir local com %d peças vinculadas. " +
                    "Mova ou remova as peças primeiro.",
                    pecasVinculadas
                )
            );
        }
        
        // Validação 2: Sem locais filhos
        List<LocalArmazenamento> filhos = localRepository.findByLocalizacaoPaiId(id);
        if (!filhos.isEmpty()) {
            throw new BusinessException(
                String.format(
                    "Não é possível excluir local com %d sub-locais. " +
                    "Exclua ou mova os sub-locais primeiro.",
                    filhos.size()
                )
            );
        }
        
        localRepository.delete(local);
        
        log.info("Local {} excluído permanentemente", id);
    }
    
    /**
     * Inativa local (soft delete).
     * Mais seguro que exclusão, permite reativar depois.
     */
    @Transactional
    @CacheEvict(value = "locais-cache", allEntries = true)
    public void inativarLocal(UUID id) {
        log.info("Inativando local {}", id);
        
        LocalArmazenamento local = localRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Local não encontrado"));
        
        // Verifica peças vinculadas
        long pecasVinculadas = localRepository.countPecasVinculadas(id);
        if (pecasVinculadas > 0) {
            throw new BusinessException(
                String.format(
                    "Não é possível inativar local com %d peças. " +
                    "Mova as peças para outro local primeiro.",
                    pecasVinculadas
                )
            );
        }
        
        local.setAtivo(false);
        localRepository.save(local);
        
        log.info("Local {} inativado com sucesso", id);
    }
    
    /**
     * Reativa local previamente inativado.
     */
    @Transactional
    @CacheEvict(value = "locais-cache", allEntries = true)
    public void reativarLocal(UUID id) {
        log.info("Reativando local {}", id);
        
        LocalArmazenamento local = localRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Local não encontrado"));
        
        if (local.getAtivo()) {
            throw new BusinessException("Local já está ativo");
        }
        
        // Se tem pai, verifica se pai está ativo
        if (local.getLocalizacaoPai() != null && !local.getLocalizacaoPai().getAtivo()) {
            throw new BusinessException(
                "Não é possível reativar local cujo pai está inativo"
            );
        }
        
        local.setAtivo(true);
        localRepository.save(local);
        
        log.info("Local {} reativado com sucesso", id);
    }
    
    /**
     * Lista estrutura hierárquica completa.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "hierarquia-locais", key = "#apenasAtivos")
    public List<LocalArmazenamentoDTO> listarHierarquia(boolean apenasAtivos) {
        List<LocalArmazenamento> raizes = apenasAtivos
            ? localRepository.findLocaisRaiz()
            : localRepository.findAll().stream()
                .filter(l -> l.getLocalizacaoPai() == null)
                .collect(Collectors.toList());
        
        return raizes.stream()
            .map(this::mapToDTOComFilhos)
            .collect(Collectors.toList());
    }
    
    // ========== MÉTODOS AUXILIARES ==========
    
    /**
     * Valida regras de hierarquia de tipos.
     * Ex: Gaveta só pode estar dentro de Armário/Prateleira
     */
    private void validarHierarquiaTipos(TipoLocal tipoFilho, LocalArmazenamento pai) {
        if (pai == null) {
            return; // Local raiz, sem restrições
        }
        
        TipoLocal tipoPai = pai.getTipo();
        
        // Regras de negócio configuráveis
        Map<TipoLocal, Set<TipoLocal>> regrasPai = Map.of(
            TipoLocal.GAVETA, Set.of(TipoLocal.ARMARIO, TipoLocal.PRATELEIRA),
            TipoLocal.CAIXA, Set.of(TipoLocal.PRATELEIRA, TipoLocal.ARMARIO, TipoLocal.DEPOSITO),
            TipoLocal.PRATELEIRA, Set.of(TipoLocal.DEPOSITO, TipoLocal.ARMARIO)
        );
        
        if (regrasPai.containsKey(tipoFilho)) {
            Set<TipoLocal> paisPermitidos = regrasPai.get(tipoFilho);
            if (!paisPermitidos.contains(tipoPai)) {
                throw new BusinessException(
                    String.format(
                        "%s não pode estar dentro de %s. Tipos permitidos: %s",
                        tipoFilho.getDescricao(),
                        tipoPai.getDescricao(),
                        paisPermitidos.stream()
                            .map(TipoLocal::getDescricao)
                            .collect(Collectors.joining(", "))
                    )
                );
            }
        }
    }
    
    /**
     * Mapeia entidade para DTO com estatísticas.
     */
    private LocalArmazenamentoDTO mapToDTO(LocalArmazenamento local) {
        long pecasVinculadas = localRepository.countPecasVinculadas(local.getId());
        
        return LocalArmazenamentoDTO.builder()
            .id(local.getId())
            .codigo(local.getCodigo())
            .tipo(local.getTipo())
            .descricao(local.getDescricao())
            .localizacaoPai(local.getLocalizacaoPai() != null
                ? LocalArmazenamentoSimplificadoDTO.builder()
                    .id(local.getLocalizacaoPai().getId())
                    .codigo(local.getLocalizacaoPai().getCodigo())
                    .descricao(local.getLocalizacaoPai().getDescricao())
                    .build()
                : null)
            .capacidadeMaxima(local.getCapacidadeMaxima())
            .capacidadeUtilizada(pecasVinculadas)
            .capacidadeDisponivel(local.getCapacidadeMaxima() != null
                ? local.getCapacidadeMaxima() - pecasVinculadas
                : null)
            .observacoes(local.getObservacoes())
            .ativo(local.getAtivo())
            .caminhoCompleto(local.getCaminhoCompleto())
            .createdAt(local.getCreatedAt())
            .updatedAt(local.getUpdatedAt())
            .podeExcluir(pecasVinculadas == 0)
            .estatisticas(calcularEstatisticas(local.getId()))
            .build();
    }
    
    /**
     * Mapeia recursivamente incluindo filhos (para hierarquia).
     */
    private LocalArmazenamentoDTO mapToDTOComFilhos(LocalArmazenamento local) {
        LocalArmazenamentoDTO dto = mapToDTO(local);
        
        // Busca e mapeia filhos recursivamente
        List<LocalArmazenamento> filhos = localRepository.findByLocalizacaoPaiId(local.getId());
        // ... adicionar campo 'filhos' no DTO se necessário
        
        return dto;
    }
    
    /**
     * Calcula estatísticas detalhadas do local.
     */
    private LocalEstatisticasDTO calcularEstatisticas(UUID localId) {
        // Query customizada ou agregação
        // Simplificado aqui, implementar via @Query no repository
        long totalPecas = localRepository.countPecasVinculadas(localId);
        
        return LocalEstatisticasDTO.builder()
            .totalPecas(totalPecas)
            .pecasDiferentes(totalPecas) // Placeholder
            .valorTotalEstoque(0.0) // Calcular via SUM(quantidade * valor)
            .percentualOcupacao(0.0) // Calcular
            .build();
    }
}

4. Controller: Endpoints REST
   javapackage com.pitstop.controller;

import com.pitstop.dto.estoque.*;
import com.pitstop.service.estoque.LocalArmazenamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/estoque/locais")
@Tag(name = "Locais de Armazenamento", description = "CRUD de prateleiras, gavetas e locais")
@RequiredArgsConstructor
public class LocalArmazenamentoController {

    private final LocalArmazenamentoService localService;
    
    @GetMapping
    @Operation(summary = "Listar locais com filtros e paginação")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    public ResponseEntity<Page<LocalArmazenamentoDTO>> listar(
        @RequestParam(required = false) TipoLocal tipo,
        @RequestParam(required = false) Boolean ativo,
        @RequestParam(required = false) String busca,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "codigo,asc") String[] sort
    ) {
        Sort.Order order = new Sort.Order(
            Sort.Direction.fromString(sort[1]), 
            sort[0]
        );
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(order));
        
        return ResponseEntity.ok(
            localService.listarLocais(tipo, ativo, busca, pageable)
        );
    }
    
    @GetMapping("/hierarquia")
    @Operation(summary = "Obter estrutura hierárquica completa")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    public ResponseEntity<List<LocalArmazenamentoDTO>> listarHierarquia(
        @RequestParam(defaultValue = "true") boolean apenasAtivos
    ) {
        return ResponseEntity.ok(
            localService.listarHierarquia(apenasAtivos)
        );
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Buscar local por ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    public ResponseEntity<LocalArmazenamentoDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(localService.buscarPorId(id));
    }
    
    @PostMapping
    @Operation(summary = "Criar novo local")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<LocalArmazenamentoDTO> criar(
        @Valid @RequestBody CriarLocalRequest request
    ) {
        LocalArmazenamentoDTO criado = localService.criarLocal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar local existente")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<LocalArmazenamentoDTO> atualizar(
        @PathVariable UUID id,
        @Valid @RequestBody AtualizarLocalRequest request
    ) {
        return ResponseEntity.ok(localService.atualizarLocal(id, request));
    }
    
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Excluir local (permanente)",
        description = "Só funciona se não houver peças vinculadas nem sub-locais"
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        localService.excluirLocal(id);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/{id}/inativar")
    @Operation(summary = "Inativar local (soft delete)")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<Void> inativar(@PathVariable UUID id) {
        localService.inativarLocal(id);
        return ResponseEntity.ok().build();
    }
    
    @PatchMapping("/{id}/reativar")
    @Operation(summary = "Reativar local")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<Void> reativar(@PathVariable UUID id) {
        localService.reativarLocal(id);
        return ResponseEntity.ok().build();
    }
}

🎨 FRONTEND: INTERFACE COMPLETA
1. Service API
   typescript// services/locais-api.ts

import { api } from './api-client';
import type {
LocalArmazenamento,
CriarLocalRequest,
AtualizarLocalRequest,
LocalArmazenamentoDTO,
TipoLocal,
PagedResponse
} from '@/types/estoque';

export const locaisApi = {
/**
* Lista locais com filtros e paginação.
  */
  async listar(params: {
  tipo?: TipoLocal;
  ativo?: boolean;
  busca?: string;
  page?: number;
  size?: number;
  sort?: string;
  }): Promise<PagedResponse<LocalArmazenamentoDTO>> {
  const { data } = await api.get('/estoque/locais', { params });
  return data;
  },

/**
* Busca hierarquia completa.
  */
  async listarHierarquia(apenasAtivos = true): Promise<LocalArmazenamentoDTO[]> {
  const { data } = await api.get('/estoque/locais/hierarquia', {
  params: { apenasAtivos }
  });
  return data;
  },

/**
* Busca local por ID.
  */
  async buscarPorId(id: string): Promise<LocalArmazenamentoDTO> {
  const { data } = await api.get(`/estoque/locais/${id}`);
  return data;
  },

/**
* Cria novo local.
  */
  async criar(request: CriarLocalRequest): Promise<LocalArmazenamentoDTO> {
  const { data } = await api.post('/estoque/locais', request);
  return data;
  },

/**
* Atualiza local existente.
  */
  async atualizar(
  id: string,
  request: AtualizarLocalRequest
  ): Promise<LocalArmazenamentoDTO> {
  const { data } = await api.put(`/estoque/locais/${id}`, request);
  return data;
  },

/**
* Exclui local (hard delete).
  */
  async excluir(id: string): Promise<void> {
  await api.delete(`/estoque/locais/${id}`);
  },

/**
* Inativa local (soft delete).
  */
  async inativar(id: string): Promise<void> {
  await api.patch(`/estoque/locais/${id}/inativar`);
  },

/**
* Reativa local.
  */
  async reativar(id: string): Promise<void> {
  await api.patch(`/estoque/locais/${id}/reativar`);
  }
  };

2. Formulário de Criação/Edição
   tsx// components/estoque/FormularioLocal.tsx

import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
Box,
Building2,
Drawer,
Package,
Warehouse,
Archive
} from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import {
Form,
FormControl,
FormDescription,
FormField,
FormItem,
FormLabel,
FormMessage
} from '@/components/ui/form';
import {
Select,
SelectContent,
SelectItem,
SelectTrigger,
SelectValue
} from '@/components/ui/select';
import { toast } from 'sonner';

import { locaisApi } from '@/services/locais-api';
import { TipoLocal } from '@/types/estoque';
import type { LocalArmazenamentoDTO } from '@/types/estoque';

// Schema de validação Zod
const localSchema = z.object({
codigo: z
.string()
.min(1, 'Código é obrigatório')
.max(50, 'Máximo 50 caracteres')
.regex(
/^[A-Z0-9-]+$/,
'Use apenas letras maiúsculas, números e hífen'
)
.transform((val) => val.toUpperCase()),

tipo: z.nativeEnum(TipoLocal, {
required_error: 'Selecione um tipo'
}),

descricao: z
.string()
.min(1, 'Descrição é obrigatória')
.max(200, 'Máximo 200 caracteres'),

localizacaoPaiId: z.string().uuid().optional().nullable(),

capacidadeMaxima: z
.number()
.int('Deve ser número inteiro')
.min(1, 'Mínimo 1')
.max(10000, 'Máximo 10.000')
.optional()
.nullable(),

observacoes: z
.string()
.max(500, 'Máximo 500 caracteres')
.optional()
.nullable()
});

type LocalFormData = z.infer<typeof localSchema>;

interface FormularioLocalProps {
localId?: string; // Se presente, modo edição
onSuccess?: () => void;
onCancel?: () => void;
}

export function FormularioLocal({
localId,
onSuccess,
onCancel
}: FormularioLocalProps) {
const queryClient = useQueryClient();
const isEditMode = !!localId;

// Busca dados do local (modo edição)
const { data: localExistente, isLoading: carregandoLocal } = useQuery({
queryKey: ['local', localId],
queryFn: () => locaisApi.buscarPorId(localId!),
enabled: isEditMode
});

// Busca locais para seleção de "pai"
const { data: locaisDisponiveis } = useQuery({
queryKey: ['locais-hierarquia'],
queryFn: () => locaisApi.listarHierarquia(true)
});

// Form setup
const form = useForm<LocalFormData>({
resolver: zodResolver(localSchema),
defaultValues: {
codigo: '',
tipo: TipoLocal.PRATELEIRA,
descricao: '',
localizacaoPaiId: null,
capacidadeMaxima: null,
observacoes: null
}
});

// Preenche form com dados existentes (edição)
useEffect(() => {
if (localExistente) {
form.reset({
codigo: localExistente.codigo,
tipo: localExistente.tipo,
descricao: localExistente.descricao,
localizacaoPaiId: localExistente.localizacaoPai?.id || null,
capacidadeMaxima: localExistente.capacidadeMaxima,
observacoes: localExistente.observacoes
});
}
}, [localExistente, form]);

// Mutation: Criar
const criarMutation = useMutation({
mutationFn: locaisApi.criar,
onSuccess: () => {
toast.success('Local criado com sucesso!');
queryClient.invalidateQueries(['locais-hierarquia']);
queryClient.invalidateQueries(['locais-armazenamento']);
onSuccess?.();
},
onError: (error: any) => {
toast.error(error.response?.data?.message || 'Erro ao criar local');
}
});

// Mutation: Atualizar
const atualizarMutation = useMutation({
mutationFn: (data: LocalFormData) =>
locaisApi.atualizar(localId!, {
...data,
ativo: localExistente?.ativo ?? true
}),
onSuccess: () => {
toast.success('Local atualizado com sucesso!');
queryClient.invalidateQueries(['locais-hierarquia']);
queryClient.invalidateQueries(['local', localId]);
onSuccess?.();
},
onError: (error: any) => {
toast.error(error.response?.data?.message || 'Erro ao atualizar local');
}
});

const onSubmit = (data: LocalFormData) => {
if (isEditMode) {
atualizarMutation.mutate(data);
} else {
criarMutation.mutate(data);
}
};

const isSubmitting = criarMutation.isPending || atualizarMutation.isPending;

if (isEditMode && carregandoLocal) {
return <div className="p-6 text-center">Carregando...</div>;
}

return (
<Form {...form}>
<form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
{/* Código */}
<FormField
control={form.control}
name="codigo"
render={({ field }) => (
<FormItem>
<FormLabel>Código *</FormLabel>
<FormControl>
<Input
placeholder="Ex: PRAT-03-A"
{...field}
className="font-mono uppercase"
/>
</FormControl>
<FormDescription>
Identificador único. Use letras maiúsculas, números e hífen.
</FormDescription>
<FormMessage />
</FormItem>
)}
/>

        {/* Tipo */}
        <FormField
          control={form.control}
          name="tipo"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Tipo *</FormLabel>
              <Select
                onValueChange={field.onChange}
                defaultValue={field.value}
              >
                <FormControl>
                  <SelectTrigger>
                    <SelectValue placeholder="Selecione o tipo" />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  {Object.entries(tipoLocalConfig).map(([key, config]) => (
                    <SelectItem key={key} value={key}>
                      <div className="flex items-center gap-2">
                        <config.icon className="h-4 w-4" />
                        <span>{config.label}</span>
                      </div>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* Descrição */}
        <FormField
          control={form.control}
          name="descricao"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Descrição *</FormLabel>
              <FormControl>
                <Input placeholder="Ex: Prateleira 3, Setor A" {...field} />
              </FormControl>
              <FormDescription>
                Nome descritivo que facilite a identificação
              </FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* Local Pai (Hierarquia) */}
        <FormField
          control={form.control}
          name="localizacaoPaiId"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Local Pai (Hierarquia)</FormLabel>
              <Select
                onValueChange={field.onChange}
                value={field.value || undefined}
              >
                <FormControl>
                  <SelectTrigger>
                    <SelectValue placeholder="Nenhum (local raiz)" />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  <SelectItem value="__null__">
                    <span className="text-muted-foreground">
                      Nenhum (local raiz)
                    </span>
                  </SelectItem>
                  {locaisDisponiveis
                    ?.filter((l) => l.id !== localId) // Não pode ser pai de si mesmo
                    .map((local) => (
                      <SelectItem key={local.id} value={local.id}>
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-xs">
                            {local.codigo}
                          </span>
                          <span>{local.descricao}</span>
                        </div>
                      </SelectItem>
                    ))}
                </SelectContent>
              </Select>
              <FormDescription>
                Ex: Gaveta dentro de Armário, Prateleira dentro de Depósito
              </FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* Capacidade Máxima */}
        <FormField
          control={form.control}
          name="capacidadeMaxima"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Capacidade Máxima</FormLabel>
              <FormControl>
                <Input
                  type="number"
                  placeholder="Ex: 50"
                  {...field}
                  value={field.value || ''}
                  onChange={(e) =>
                    field.onChange(
                      e.target.value ? parseInt(e.target.value) : null
                    )
                  }
                />
              </FormControl>
              <FormDescription>
                Limite de itens que podem ser armazenados (opcional)
              </FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* Observações */}
        <FormField
          control={form.control}
          name="observacoes"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Observações</FormLabel>
              <FormControl>
                <Textarea
                  placeholder="Informações adicionais..."
                  className="resize-none"
                  rows={3}
                  {...field}
                  value={field.value || ''}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* Ações */}
        <div className="flex items-center justify-end gap-3 pt-4 border-t">
          {onCancel && (
            <Button type="button" variant="outline" onClick={onCancel}>
              Cancelar
            </Button>
          )}
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting
              ? 'Salvando...'
              : isEditMode
              ? 'Atualizar Local'
              : 'Criar Local'}
          </Button>
        </div>
      </form>
    </Form>
);
}

// Configuração visual dos tipos
const tipoLocalConfig = {
[TipoLocal.PRATELEIRA]: {
label: 'Prateleira',
icon: Building2
},
[TipoLocal.GAVETA]: {
label: 'Gaveta',
icon: Drawer
},
[TipoLocal.ARMARIO]: {
label: 'Armário',
icon: Archive
},
[TipoLocal.DEPOSITO]: {
label: 'Depósito',
icon: Warehouse
},
[TipoLocal.CAIXA]: {
label: 'Caixa Organizadora',
icon: Box
},
[TipoLocal.VITRINE]: {
label: 'Vitrine',
icon: Package
},
[TipoLocal.OUTRO]: {
label: 'Outro',
icon: Package
}
};

📱 FEATURES EXTRAS
1. Importação em Massa (CSV)
   typescript// components/estoque/ImportarLocaisCSV.tsx

interface ImportacaoCSV {
codigo: string;
tipo: string;
descricao: string;
codigoPai?: string;
capacidade?: number;
}

export function ImportarLocaisCSV() {
const [arquivo, setArquivo] = useState<File | null>(null);

const processarCSV = async (file: File) => {
const texto = await file.text();
const linhas = texto.split('\n');

    // Parsear CSV (usar biblioteca como papaparse)
    const locais: ImportacaoCSV[] = parseCSV(linhas);
    
    // Validar e criar em batch
    for (const local of locais) {
      await locaisApi.criar({
        codigo: local.codigo,
        tipo: local.tipo as TipoLocal,
        descricao: local.descricao,
        // ... resolver hierarquia via codigoPai
      });
    }
};

return (
<div>
<input
type="file"
accept=".csv"
onChange={(e) => setArquivo(e.target.files?.[0] || null)}
/>
<Button onClick={() => arquivo && processarCSV(arquivo)}>
Importar Locais
</Button>
</div>
);
}
2. Gerador de Etiquetas
   tsx// Gera etiquetas com QR Code para impressão
   export function GerarEtiquetas({ localId }: { localId: string }) {
   const gerarPDF = async () => {
   // Usar biblioteca como jsPDF + qrcode.react
   const doc = new jsPDF({
   orientation: 'portrait',
   unit: 'mm',
   format: [50, 30] // Etiqueta 50x30mm
   });

   // Adicionar código + QR Code
   doc.text(local.codigo, 10, 10);
   // doc.addImage(qrCodeDataURL, 'PNG', 10, 15, 20, 20);

   doc.save(`etiqueta-${local.codigo}.pdf`);
   };

return <Button onClick={gerarPDF}>Gerar Etiqueta PDF</Button>;
}

🎓 JUSTIFICATIVAS ARQUITETURAIS
Por que Soft Delete (inativar) em vez de Hard Delete?
Realidade: Oficinas reorganizam layouts constantemente

✅ Histórico preservado: Saber onde peças estavam antes
✅ Recuperação: Reativar local se reorganização foi erro
✅ Auditoria: Rastreabilidade completa
✅ Dados vinculados: Não quebra referências históricas

Por que Validação de Hierarquia no Backend?
Segurança e Integridade

🚫 Previne ciclos (A → B → A)
🚫 Valida regras de negócio (gaveta só em armário)
✅ Frontend pode falhar, backend é verdade absoluta
✅ API segura para mobile/integrações futuras

Por que Cache em Redis?
Performance

Hierarquia raramente muda → TTL 1 hora
Evita N queries recursivas
Invalidação explícita após CUD

Por que Código Único e Normalizado?
UX e Consistência

Usuário digita "prat-3-a" → salva "PRAT-3-A"
Facilita busca case-insensitive
Padrão visual uniforme em etiquetas


✅ CHECKLIST DE IMPLEMENTAÇÃO
Backend

Migration: tabela local_armazenamento
Entity com relacionamento auto-referencial
Repository com queries customizadas
Service com validações de negócio
Controller REST completo
Testes unitários e integração
Liquibase changeset

Frontend

Types TypeScript
Service API (Axios)
Formulário com React Hook Form + Zod
Listagem com hierarquia visual
Modais de confirmação (excluir/inativar)
Importação CSV
Gerador de etiquetas PDF

Extras (Futuro)

App mobile com scanner QR Code
Dashboard de ocupação (ECharts)
IA para sugestão de localização
Fotos dos locais físicos


🎯 Resultado: Sistema completo e flexível que permite ao cliente gerenciar seu layout físico de forma profissional, adaptando-se às mudanças constantes do ambiente da oficina.