import {
  Background, BackgroundVariant, Controls, Handle, MarkerType, MiniMap, Position, ReactFlow,
  useEdgesState, useNodesState, type Connection, type Edge, type Node, type NodeProps,
  type ReactFlowInstance,
} from '@xyflow/react'
import {
  ArrowRight, Check, ChevronDown, ChevronRight, CircleAlert, CircleDot, Copy, FileJson,
  FolderTree, ImagePlus, Layers3, Link2, LoaderCircle, Maximize2, Menu, Network, PanelRight,
  Plus, RefreshCw, Save, Search, Settings2, Trash2, Unlink, X,
} from 'lucide-react'
import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { api, ApiError } from './api'
import type {
  JsonValue, NarrativeDto, NarrativeLayer, NarrativeNode, NarrativeSearchItem, NarrativeTree,
  NarrativeTreeItem, PositionDto,
} from './types'
import { extractAssets, extractText, withAsset, withText } from './types'
import './App.css'

const DEFAULT_PROJECT_ID = 'd101059d-ec15-429a-a29d-fcd0ee47cefc'

type FlowNodeData = {
  title: string
  excerpt: string
  childLayerId: string | null
  references: number
  onOpenChild: (layerId: string) => void
}
type NarrativeFlowNode = Node<FlowNodeData, 'narrative'>
type NarrativeFlowEdge = Edge
type Toast = { id: number; kind: 'success' | 'error' | 'neutral'; message: string }
type AppStatus = 'loading' | 'ready' | 'error'

function NarrativeCard({ data, selected }: NodeProps<NarrativeFlowNode>) {
  return (
    <div className={`narrative-node${selected ? ' is-selected' : ''}`}>
      <Handle className="node-handle" type="target" position={Position.Left} />
      <div className="node-accent" />
      <div className="node-header">
        <span className="node-type"><CircleDot size={13} /> Сюжетный узел</span>
        {data.childLayerId && (
          <button
            aria-label="Открыть вложенный слой"
            className="node-layer-button nodrag"
            onClick={(event) => { event.stopPropagation(); data.onOpenChild(data.childLayerId as string) }}
            onMouseDown={(event) => event.stopPropagation()}
            title="Открыть вложенный слой"
            type="button"
          ><Layers3 size={15} /></button>
        )}
      </div>
      <strong className="node-title">{data.title}</strong>
      <p className="node-excerpt">{data.excerpt || 'Пока без описания'}</p>
      <div className="node-meta">
        <span><Link2 size={13} /> {data.references}</span>
        {data.childLayerId && <span>Есть слой <ArrowRight size={13} /></span>}
      </div>
      <Handle className="node-handle" type="source" position={Position.Right} />
    </div>
  )
}

const nodeTypes = { narrative: NarrativeCard }

function Spinner({ label = 'Загрузка' }: { label?: string }) {
  return <LoaderCircle aria-label={label} className="spinner" size={18} />
}

function EmptyState({ icon, title, text, action }: {
  icon: ReactNode; title: string; text: string; action?: ReactNode
}) {
  return (
    <div className="empty-state">
      <div className="empty-icon">{icon}</div>
      <strong>{title}</strong>
      <p>{text}</p>
      {action}
    </div>
  )
}

function TreeBranch({ item, childrenByParent, activeNodeId, activeLayerId, onOpenNode, onOpenLayer, depth = 0 }: {
  item: NarrativeTreeItem
  childrenByParent: Map<string, NarrativeTreeItem[]>
  activeNodeId: string | null
  activeLayerId: string | null
  onOpenNode: (item: NarrativeTreeItem) => void
  onOpenLayer: (layerId: string) => void
  depth?: number
}) {
  const children = childrenByParent.get(item.id) ?? []
  const [expanded, setExpanded] = useState(true)
  const hasChildren = Boolean(item.childLayerId) || children.length > 0
  return (
    <div className="tree-branch">
      <div className={`tree-row${activeNodeId === item.id ? ' is-active' : ''}`} style={{ paddingLeft: 10 + depth * 14 }}>
        <button
          aria-label={expanded ? 'Свернуть ветку' : 'Развернуть ветку'}
          className="tree-toggle"
          disabled={!hasChildren}
          onClick={() => setExpanded((value) => !value)}
          type="button"
        >{hasChildren ? (expanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />) : <span />}</button>
        <button className="tree-node-button" onClick={() => onOpenNode(item)} type="button">
          <span className="tree-node-dot" /><span>{item.title}</span>
        </button>
        {item.childLayerId && (
          <button
            aria-label={`Открыть слой узла ${item.title}`}
            className={`tree-layer-button${activeLayerId === item.childLayerId ? ' is-current' : ''}`}
            onClick={() => onOpenLayer(item.childLayerId as string)}
            title="Открыть вложенный слой"
            type="button"
          ><Layers3 size={14} /></button>
        )}
      </div>
      {expanded && children.map((child) => (
        <TreeBranch
          activeLayerId={activeLayerId} activeNodeId={activeNodeId} childrenByParent={childrenByParent}
          depth={depth + 1} item={child} key={child.id} onOpenLayer={onOpenLayer} onOpenNode={onOpenNode}
        />
      ))}
    </div>
  )
}

function Modal({ title, children, onClose, width = 'medium' }: {
  title: string; children: ReactNode; onClose: () => void; width?: 'small' | 'medium'
}) {
  return (
    <div className="modal-backdrop" onMouseDown={onClose} role="presentation">
      <section aria-modal="true" className={`modal modal-${width}`} onMouseDown={(event) => event.stopPropagation()} role="dialog">
        <header className="modal-header">
          <h2>{title}</h2>
          <button aria-label="Закрыть" className="icon-button" onClick={onClose} type="button"><X size={18} /></button>
        </header>
        {children}
      </section>
    </div>
  )
}

function formatError(error: unknown): string {
  if (error instanceof ApiError || error instanceof Error) return error.message
  return 'Не удалось выполнить запрос'
}

function getStoredProjectId() {
  return window.localStorage.getItem('emosdk.projectId') || DEFAULT_PROJECT_ID
}

function uuidIsValid(value: string) {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value)
}

function App() {
  const [projectId, setProjectId] = useState(getStoredProjectId)
  const [projectDraft, setProjectDraft] = useState(projectId)
  const [status, setStatus] = useState<AppStatus>('loading')
  const [statusMessage, setStatusMessage] = useState('Подключаемся к narrative API')
  const [narrative, setNarrative] = useState<NarrativeDto | null>(null)
  const [tree, setTree] = useState<NarrativeTree>({ items: [] })
  const [layer, setLayer] = useState<NarrativeLayer | null>(null)
  const [currentLayerId, setCurrentLayerId] = useState<string | null>(null)
  const [layerLoading, setLayerLoading] = useState(false)
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null)
  const [selectedNode, setSelectedNode] = useState<NarrativeNode | null>(null)
  const [nodeLoading, setNodeLoading] = useState(false)
  const [nodes, setNodes, onNodesChange] = useNodesState<NarrativeFlowNode>([])
  const [edges, setEdges, onEdgesChange] = useEdgesState<NarrativeFlowEdge>([])
  const [flowInstance, setFlowInstance] = useState<ReactFlowInstance<NarrativeFlowNode, NarrativeFlowEdge> | null>(null)
  const [saveState, setSaveState] = useState<'idle' | 'saving' | 'saved'>('idle')
  const [editorMode, setEditorMode] = useState<'text' | 'json'>('text')
  const [editorTitle, setEditorTitle] = useState('')
  const [editorText, setEditorText] = useState('')
  const [editorJson, setEditorJson] = useState('{}')
  const [linkedNodeIds, setLinkedNodeIds] = useState<string[]>([])
  const [savingNode, setSavingNode] = useState(false)
  const [assetUploading, setAssetUploading] = useState(false)
  const [showCreateNode, setShowCreateNode] = useState(false)
  const [newNodeTitle, setNewNodeTitle] = useState('')
  const [newNodeText, setNewNodeText] = useState('')
  const [creatingNode, setCreatingNode] = useState(false)
  const [showProjectDialog, setShowProjectDialog] = useState(false)
  const [projectSwitching, setProjectSwitching] = useState(false)
  const [showDeleteDialog, setShowDeleteDialog] = useState(false)
  const [deletingNode, setDeletingNode] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState<NarrativeSearchItem[]>([])
  const [searchTotal, setSearchTotal] = useState(0)
  const [searchLoading, setSearchLoading] = useState(false)
  const [searchOpen, setSearchOpen] = useState(false)
  const [leftOpen, setLeftOpen] = useState(false)
  const [rightOpen, setRightOpen] = useState(false)
  const [toasts, setToasts] = useState<Toast[]>([])
  const canvasRef = useRef<HTMLDivElement>(null)
  const assetInputRef = useRef<HTMLInputElement>(null)

  const notify = useCallback((message: string, kind: Toast['kind'] = 'neutral') => {
    const id = Date.now() + Math.random()
    setToasts((current) => [...current, { id, kind, message }])
    window.setTimeout(() => setToasts((current) => current.filter((toast) => toast.id !== id)), 3600)
  }, [])

  const loadProject = useCallback(async (nextProjectId: string) => {
    setStatus('loading')
    setStatusMessage('Загружаем структуру проекта')
    setSelectedNodeId(null)
    setSelectedNode(null)
    try {
      const [nextNarrative, nextTree] = await Promise.all([
        api.getNarrative(nextProjectId), api.getTree(nextProjectId),
      ])
      const rootLayer = await api.getLayer(nextProjectId, nextNarrative.rootLayerId)
      setProjectId(nextProjectId)
      setProjectDraft(nextProjectId)
      setNarrative(nextNarrative)
      setTree(nextTree)
      setLayer(rootLayer)
      setCurrentLayerId(rootLayer.id)
      setStatus('ready')
      window.localStorage.setItem('emosdk.projectId', nextProjectId)
    } catch (error) {
      setNarrative(null)
      setTree({ items: [] })
      setLayer(null)
      setCurrentLayerId(null)
      setStatusMessage(formatError(error))
      setStatus('error')
      throw error
    }
  }, [])

  useEffect(() => { void loadProject(getStoredProjectId()).catch(() => undefined) }, [loadProject])

  const openLayer = useCallback(async (layerId: string, nodeToSelect: string | null = null) => {
    setLayerLoading(true)
    setLeftOpen(false)
    if (nodeToSelect) setRightOpen(true)
    try {
      const nextLayer = await api.getLayer(projectId, layerId)
      setLayer(nextLayer)
      setCurrentLayerId(nextLayer.id)
      setSelectedNodeId(nodeToSelect)
      setSelectedNode(null)
    } catch (error) {
      notify(formatError(error), 'error')
    } finally {
      setLayerLoading(false)
    }
  }, [notify, projectId])

  const refreshLayer = useCallback(async (withTree = false) => {
    if (!currentLayerId) return
    try {
      const layerRequest = api.getLayer(projectId, currentLayerId)
      const treeRequest = withTree ? api.getTree(projectId) : null
      setLayer(await layerRequest)
      if (treeRequest) setTree(await treeRequest)
    } catch (error) {
      notify(formatError(error), 'error')
    }
  }, [currentLayerId, notify, projectId])

  const openChildLayer = useCallback((childLayerId: string) => { void openLayer(childLayerId) }, [openLayer])

  useEffect(() => {
    if (!layer) { setNodes([]); setEdges([]); return }
    const visibleIds = new Set(layer.nodes.map((node) => node.id))
    const referenceCounts = new Map<string, number>()
    for (const reference of layer.references) {
      referenceCounts.set(reference.sourceNodeId, (referenceCounts.get(reference.sourceNodeId) ?? 0) + 1)
    }
    setNodes(layer.nodes.map((node) => ({
      id: node.id,
      type: 'narrative',
      position: node.position,
      data: {
        title: node.title, excerpt: node.excerpt, childLayerId: node.childLayerId,
        references: referenceCounts.get(node.id) ?? 0, onOpenChild: openChildLayer,
      },
    })))
    setEdges(layer.references
      .filter((reference) => visibleIds.has(reference.sourceNodeId) && visibleIds.has(reference.targetNodeId))
      .map((reference) => ({
        id: `reference-${reference.sourceNodeId}-${reference.targetNodeId}`,
        source: reference.sourceNodeId,
        target: reference.targetNodeId,
        type: 'smoothstep',
        markerEnd: { type: MarkerType.ArrowClosed, color: '#167a65' },
        style: { stroke: '#167a65', strokeWidth: 2 },
      })))
  }, [layer, openChildLayer, setEdges, setNodes])

  useEffect(() => {
    if (!selectedNodeId || !layer) { setSelectedNode(null); return }
    let active = true
    setNodeLoading(true)
    void api.getNode(projectId, selectedNodeId)
      .then((node) => {
        if (!active) return
        setSelectedNode(node)
        setEditorTitle(node.title)
        setEditorText(extractText(node.content))
        setEditorJson(JSON.stringify(node.content, null, 2))
        setLinkedNodeIds(layer.references
          .filter((reference) => reference.sourceNodeId === node.id)
          .map((reference) => reference.targetNodeId))
        setEditorMode('text')
      })
      .catch((error) => { if (active) notify(formatError(error), 'error') })
      .finally(() => { if (active) setNodeLoading(false) })
    return () => { active = false }
  }, [layer, notify, projectId, selectedNodeId])

  useEffect(() => {
    const query = searchQuery.trim()
    if (query.length < 2) {
      setSearchResults([]); setSearchTotal(0); setSearchLoading(false); return
    }
    let active = true
    const timer = window.setTimeout(() => {
      setSearchLoading(true)
      void api.search(projectId, query)
        .then((result) => {
          if (!active) return
          setSearchResults(result.items)
          setSearchTotal(result.totalElements)
        })
        .catch((error) => { if (active) notify(formatError(error), 'error') })
        .finally(() => { if (active) setSearchLoading(false) })
    }, 320)
    return () => { active = false; window.clearTimeout(timer) }
  }, [notify, projectId, searchQuery])

  const treeTitleMap = useMemo(() => new Map(tree.items.map((item) => [item.id, item.title])), [tree.items])
  const childrenByParent = useMemo(() => {
    const map = new Map<string, NarrativeTreeItem[]>()
    for (const item of tree.items) {
      if (!item.parentNodeId) continue
      const children = map.get(item.parentNodeId) ?? []
      children.push(item)
      map.set(item.parentNodeId, children)
    }
    return map
  }, [tree.items])
  const rootTreeItems = useMemo(() => tree.items.filter((item) => !item.parentNodeId), [tree.items])
  const availableLinkTargets = useMemo(
    () => tree.items.filter((item) => item.id !== selectedNodeId && !linkedNodeIds.includes(item.id)),
    [linkedNodeIds, selectedNodeId, tree.items],
  )
  const parsedEditorContent = useMemo(() => {
    try { return JSON.parse(editorJson) as JsonValue } catch { return null }
  }, [editorJson])
  const editorAssets = useMemo(() => extractAssets(parsedEditorContent), [parsedEditorContent])

  const updateNodeLinks = useCallback(async (sourceNodeId: string, nextLinkedIds: string[]) => {
    const node = await api.getNode(projectId, sourceNodeId)
    await api.updateNode(projectId, sourceNodeId, {
      title: node.title, content: node.content, linkedNodeIds: [...new Set(nextLinkedIds)],
    })
  }, [projectId])

  const handleConnect = useCallback(async (connection: Connection) => {
    if (!connection.source || !connection.target || connection.source === connection.target || !layer) return
    const currentLinks = layer.references
      .filter((reference) => reference.sourceNodeId === connection.source)
      .map((reference) => reference.targetNodeId)
    if (currentLinks.includes(connection.target)) return
    try {
      await updateNodeLinks(connection.source, [...currentLinks, connection.target])
      await refreshLayer()
      notify('Связь создана', 'success')
    } catch (error) { notify(formatError(error), 'error') }
  }, [layer, notify, refreshLayer, updateNodeLinks])

  const handleEdgesDelete = useCallback(async (deletedEdges: NarrativeFlowEdge[]) => {
    if (!layer) return
    const targetsBySource = new Map<string, Set<string>>()
    for (const edge of deletedEdges) {
      const targets = targetsBySource.get(edge.source) ?? new Set<string>()
      targets.add(edge.target)
      targetsBySource.set(edge.source, targets)
    }
    try {
      await Promise.all([...targetsBySource.entries()].map(([source, deletedTargets]) => {
        const remaining = layer.references
          .filter((reference) => reference.sourceNodeId === source && !deletedTargets.has(reference.targetNodeId))
          .map((reference) => reference.targetNodeId)
        return updateNodeLinks(source, remaining)
      }))
      await refreshLayer()
      notify('Связь удалена', 'success')
    } catch (error) { notify(formatError(error), 'error'); await refreshLayer() }
  }, [layer, notify, refreshLayer, updateNodeLinks])

  const handlePositionSave = useCallback(async (node: NarrativeFlowNode) => {
    setSaveState('saving')
    try {
      await api.updatePositions(projectId, [{ id: node.id, position: node.position }])
      setSaveState('saved')
      window.setTimeout(() => setSaveState('idle'), 1600)
    } catch (error) { setSaveState('idle'); notify(formatError(error), 'error') }
  }, [notify, projectId])

  const getCreatePosition = (): PositionDto => {
    const rect = canvasRef.current?.getBoundingClientRect()
    if (flowInstance && rect) {
      return flowInstance.screenToFlowPosition({ x: rect.left + rect.width / 2, y: rect.top + rect.height / 2 })
    }
    return { x: 120 + nodes.length * 24, y: 120 + nodes.length * 18 }
  }

  const handleCreateNode = async () => {
    if (!currentLayerId || !newNodeTitle.trim()) return
    setCreatingNode(true)
    try {
      const created = await api.createNode(projectId, {
        layerId: currentLayerId, title: newNodeTitle.trim(), content: { text: newNodeText.trim() },
        position: getCreatePosition(), linkedNodeIds: [],
      })
      await refreshLayer(true)
      setSelectedNodeId(created.id)
      setRightOpen(true)
      setNewNodeTitle('')
      setNewNodeText('')
      setShowCreateNode(false)
      notify('Узел создан', 'success')
    } catch (error) { notify(formatError(error), 'error') }
    finally { setCreatingNode(false) }
  }

  const handleSaveNode = async () => {
    if (!selectedNode || !editorTitle.trim()) return
    let content: JsonValue
    try {
      const current = JSON.parse(editorJson) as JsonValue
      content = editorMode === 'json' ? current : withText(current, editorText)
    } catch {
      notify('Исправьте JSON перед сохранением', 'error')
      setEditorMode('json')
      return
    }
    setSavingNode(true)
    try {
      const updated = await api.updateNode(projectId, selectedNode.id, {
        title: editorTitle.trim(), content, linkedNodeIds,
      })
      setSelectedNode(updated)
      setEditorJson(JSON.stringify(updated.content, null, 2))
      setEditorText(extractText(updated.content))
      await refreshLayer(true)
      notify('Изменения сохранены', 'success')
    } catch (error) { notify(formatError(error), 'error') }
    finally { setSavingNode(false) }
  }

  const handleAssetUpload = async (file: File | undefined) => {
    if (!file) return
    setAssetUploading(true)
    try {
      const asset = await api.uploadAsset(projectId, file)
      let current: JsonValue = {}
      try { current = JSON.parse(editorJson) as JsonValue } catch { current = { text: editorText } }
      setEditorJson(JSON.stringify(withAsset(current, asset), null, 2))
      notify('Изображение добавлено в контент', 'success')
    } catch (error) { notify(formatError(error), 'error') }
    finally {
      setAssetUploading(false)
      if (assetInputRef.current) assetInputRef.current.value = ''
    }
  }

  const handleCreateOrOpenChild = async () => {
    if (!selectedNode) return
    if (selectedNode.childLayerId) { await openLayer(selectedNode.childLayerId); return }
    try {
      const child = await api.createChildLayer(projectId, selectedNode.id)
      setTree(await api.getTree(projectId))
      await openLayer(child.id)
      notify('Вложенный слой создан', 'success')
    } catch (error) { notify(formatError(error), 'error') }
  }

  const handleDeleteNode = async () => {
    if (!selectedNode) return
    setDeletingNode(true)
    try {
      await api.deleteNode(projectId, selectedNode.id)
      setSelectedNodeId(null)
      setSelectedNode(null)
      setShowDeleteDialog(false)
      setRightOpen(false)
      await refreshLayer(true)
      notify('Узел удален', 'success')
    } catch (error) { notify(formatError(error), 'error') }
    finally { setDeletingNode(false) }
  }

  const handleProjectSwitch = async () => {
    const nextId = projectDraft.trim()
    if (!uuidIsValid(nextId)) { notify('Введите корректный UUID проекта', 'error'); return }
    setProjectSwitching(true)
    try {
      await loadProject(nextId)
      setShowProjectDialog(false)
      notify('Проект открыт', 'success')
    } catch (error) { notify(formatError(error), 'error') }
    finally { setProjectSwitching(false) }
  }

  const handleTreeNodeOpen = async (item: NarrativeTreeItem) => { await openLayer(item.layerId, item.id) }
  const handleSearchResultOpen = async (item: NarrativeSearchItem) => {
    await openLayer(item.layerId, item.id)
    setSearchOpen(false)
    setSearchQuery('')
  }
  const copyNodeId = async () => {
    if (!selectedNode) return
    await navigator.clipboard.writeText(selectedNode.id)
    notify('ID скопирован', 'success')
  }
  const projectLabel = layer?.breadcrumbs[0]?.title || 'Narrative Project'
  const currentLayerLabel = layer?.breadcrumbs.at(-1)?.title || 'Корневой слой'

  if (status !== 'ready') {
    return (
      <main className="connection-screen">
        <section className="connection-panel">
          <div className="brand-mark"><Network size={24} /></div>
          <span className="brand-wordmark">EMO Narrative</span>
          {status === 'loading' ? (
            <><Spinner label="Подключение" /><h1>Открываем проект</h1><p>{statusMessage}</p></>
          ) : (
            <>
              <div className="connection-error"><CircleAlert size={22} /></div>
              <h1>Проект недоступен</h1><p>{statusMessage}</p>
              <div className="connection-actions">
                <button className="primary-button" onClick={() => void loadProject(projectId).catch(() => undefined)} type="button"><RefreshCw size={16} /> Повторить</button>
                <button className="secondary-button" onClick={() => setShowProjectDialog(true)} type="button"><Settings2 size={16} /> Другой проект</button>
              </div>
            </>
          )}
        </section>
        {showProjectDialog && <ProjectModal {...{ projectDraft, setProjectDraft, projectSwitching, handleProjectSwitch }} onClose={() => setShowProjectDialog(false)} />}
        <ToastStack toasts={toasts} />
      </main>
    )
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <div className="brand-block">
          <button aria-label="Открыть структуру" className="icon-button mobile-only" onClick={() => { setRightOpen(false); setLeftOpen(true) }} type="button"><Menu size={19} /></button>
          <div className="brand-mark small"><Network size={18} /></div>
          <div>
            <span className="brand-wordmark compact">EMO Narrative</span>
            <button className="project-name" onClick={() => setShowProjectDialog(true)} type="button">{projectLabel} <ChevronDown size={13} /></button>
          </div>
        </div>

        <div className="global-search">
          <Search className="search-icon" size={17} />
          <input
            aria-label="Поиск по narrative"
            onChange={(event) => { setSearchQuery(event.target.value); setSearchOpen(true) }}
            onFocus={() => setSearchOpen(true)}
            placeholder="Поиск по всем узлам"
            value={searchQuery}
          />
          {searchLoading && <Spinner label="Поиск" />}
          {searchQuery && !searchLoading && <button aria-label="Очистить поиск" className="search-clear" onClick={() => setSearchQuery('')} type="button"><X size={15} /></button>}
          {searchOpen && searchQuery.trim().length >= 2 && (
            <div className="search-results">
              <div className="search-results-header"><span>Результаты</span><span>{searchTotal}</span></div>
              {searchResults.map((item) => (
                <button className="search-result" key={item.id} onClick={() => void handleSearchResultOpen(item)} type="button">
                  <strong>{item.title}</strong><span>{extractText(item.content) || 'Без описания'}</span>
                </button>
              ))}
              {!searchLoading && searchResults.length === 0 && <div className="search-empty">Ничего не найдено</div>}
            </div>
          )}
        </div>

        <div className="topbar-actions">
          <span className="connection-badge"><span /> API подключен</span>
          <button aria-label="Настройки проекта" className="icon-button" onClick={() => setShowProjectDialog(true)} title="Сменить проект" type="button"><Settings2 size={18} /></button>
          <button aria-label="Открыть редактор" className="icon-button mobile-only" onClick={() => { setLeftOpen(false); setRightOpen(true) }} type="button"><PanelRight size={19} /></button>
        </div>
      </header>

      <div className="workspace">
        <aside className={`tree-panel${leftOpen ? ' is-mobile-open' : ''}`}>
          <div className="panel-heading">
            <div><span className="eyebrow">Структура</span><h2>Карта проекта</h2></div>
            <button aria-label="Закрыть структуру" className="icon-button mobile-only" onClick={() => setLeftOpen(false)} type="button"><X size={18} /></button>
          </div>
          <button className={`root-layer-button${currentLayerId === narrative?.rootLayerId ? ' is-active' : ''}`} onClick={() => narrative && void openLayer(narrative.rootLayerId)} type="button">
            <FolderTree size={16} /><span><strong>{projectLabel}</strong><small>Корневой слой</small></span>
          </button>
          <div className="tree-scroll">
            {rootTreeItems.map((item) => (
              <TreeBranch
                activeLayerId={currentLayerId} activeNodeId={selectedNodeId} childrenByParent={childrenByParent}
                item={item} key={item.id} onOpenLayer={(id) => void openLayer(id)}
                onOpenNode={(next) => void handleTreeNodeOpen(next)}
              />
            ))}
            {rootTreeItems.length === 0 && <div className="tree-empty">Первый узел появится здесь</div>}
          </div>
          <div className="tree-footer">
            <span>{tree.items.length} узлов</span><span>{1 + tree.items.filter((item) => item.childLayerId).length} слоев</span>
          </div>
        </aside>

        <section className="canvas-column">
          <div className="canvas-toolbar">
            <nav aria-label="Навигация по слоям" className="breadcrumbs">
              {layer?.breadcrumbs.map((breadcrumb, index) => (
                <span key={`${breadcrumb.layerId}-${index}`}>
                  {index > 0 && <ChevronRight size={14} />}
                  <button onClick={() => void openLayer(breadcrumb.layerId)} type="button">{breadcrumb.title}</button>
                </span>
              ))}
            </nav>
            <div className="canvas-actions">
              <span className={`save-indicator save-${saveState}`}>
                {saveState === 'saving' && <Spinner label="Сохранение координат" />}
                {saveState === 'saved' && <Check size={14} />}
                {saveState === 'idle' ? `${nodes.length} узлов` : saveState === 'saving' ? 'Сохраняем' : 'Сохранено'}
              </span>
              <button aria-label="Показать весь граф" className="icon-button" onClick={() => flowInstance?.fitView({ padding: 0.25, duration: 350 })} title="Показать весь граф" type="button"><Maximize2 size={17} /></button>
              <button className="primary-button compact-button" onClick={() => setShowCreateNode(true)} type="button"><Plus size={16} /> Добавить узел</button>
            </div>
          </div>

          <div className="canvas-wrap" ref={canvasRef}>
            {layerLoading && <div className="canvas-loading"><Spinner label="Загрузка слоя" /></div>}
            <ReactFlow<NarrativeFlowNode, NarrativeFlowEdge>
              colorMode="light"
              connectionLineStyle={{ stroke: '#167a65', strokeWidth: 2 }}
              deleteKeyCode={['Backspace', 'Delete']}
              edges={edges}
              fitView
              fitViewOptions={{ padding: 0.24 }}
              maxZoom={1.7}
              minZoom={0.25}
              nodeTypes={nodeTypes}
              nodes={nodes}
              onConnect={(connection) => void handleConnect(connection)}
              onEdgesChange={onEdgesChange}
              onEdgesDelete={(deleted) => void handleEdgesDelete(deleted)}
              onInit={(instance) => setFlowInstance(instance)}
              onNodeClick={(_, node) => { setSelectedNodeId(node.id); setLeftOpen(false); setRightOpen(true) }}
              onNodeDoubleClick={(_, node) => { if (node.data.childLayerId) void openLayer(node.data.childLayerId) }}
              onNodeDragStop={(_, node) => void handlePositionSave(node)}
              onNodesChange={onNodesChange}
              onPaneClick={() => setSelectedNodeId(null)}
              proOptions={{ hideAttribution: true }}
              selectionOnDrag
            >
              <Background color="#bcc5bf" gap={22} size={1.2} variant={BackgroundVariant.Dots} />
              <Controls position="bottom-left" showInteractive={false} />
              <MiniMap
                className="flow-minimap" maskColor="rgba(240, 243, 240, 0.78)"
                nodeColor={(node) => node.selected ? '#dc5d49' : '#167a65'} pannable position="bottom-right" zoomable
              />
            </ReactFlow>
            {nodes.length === 0 && !layerLoading && (
              <div className="canvas-empty-overlay">
                <EmptyState
                  action={<button className="primary-button" onClick={() => setShowCreateNode(true)} type="button"><Plus size={16} /> Создать узел</button>}
                  icon={<Network size={23} />} text="Начните карту этого слоя с первого смыслового блока." title="Слой пока пуст"
                />
              </div>
            )}
            <div className="layer-chip"><Layers3 size={14} /> {currentLayerLabel}</div>
          </div>
        </section>

        <aside className={`editor-panel${rightOpen ? ' is-mobile-open' : ''}`}>
          <div className="panel-heading editor-heading">
            <div><span className="eyebrow">Инспектор</span><h2>{selectedNode ? 'Редактор узла' : 'Детали'}</h2></div>
            <button aria-label="Закрыть редактор" className="icon-button mobile-only" onClick={() => setRightOpen(false)} type="button"><X size={18} /></button>
          </div>
          {!selectedNodeId && <EmptyState icon={<PanelRight size={22} />} text="Выберите карточку на графе или в структуре проекта." title="Узел не выбран" />}
          {selectedNodeId && nodeLoading && <div className="panel-loader"><Spinner label="Загрузка узла" /></div>}
          {selectedNode && !nodeLoading && (
            <div className="editor-scroll">
              <div className="node-id-row">
                <span>{selectedNode.id.slice(0, 8)}</span>
                <button aria-label="Скопировать ID" className="bare-icon-button" onClick={() => void copyNodeId()} title="Скопировать ID" type="button"><Copy size={14} /></button>
              </div>
              <label className="field-label" htmlFor="node-title">Название</label>
              <input className="text-input" id="node-title" onChange={(event) => setEditorTitle(event.target.value)} value={editorTitle} />
              <div className="field-header">
                <span className="field-label">Содержание</span>
                <div className="segmented-control" role="group">
                  <button className={editorMode === 'text' ? 'is-active' : ''} onClick={() => setEditorMode('text')} type="button">Текст</button>
                  <button className={editorMode === 'json' ? 'is-active' : ''} onClick={() => setEditorMode('json')} type="button"><FileJson size={13} /> JSON</button>
                </div>
              </div>
              {editorMode === 'text'
                ? <textarea className="content-textarea" onChange={(event) => setEditorText(event.target.value)} rows={9} value={editorText} />
                : <textarea className="content-textarea code-textarea" onChange={(event) => setEditorJson(event.target.value)} rows={12} spellCheck={false} value={editorJson} />}

              <section className="editor-section">
                <div className="section-title-row">
                  <span className="field-label"><ImagePlus size={14} /> Изображения</span>
                  <button className="small-action-button" disabled={assetUploading} onClick={() => assetInputRef.current?.click()} type="button">
                    {assetUploading ? <Spinner /> : <Plus size={14} />} Добавить
                  </button>
                  <input
                    accept="image/png,image/jpeg,image/gif,image/webp" hidden
                    onChange={(event) => void handleAssetUpload(event.target.files?.[0])}
                    ref={assetInputRef} type="file"
                  />
                </div>
                {editorAssets.length > 0 ? (
                  <div className="asset-grid">
                    {editorAssets.map((asset, index) => (
                      <div className="asset-thumb" key={`${asset.url}-${index}`}>
                        <img alt={`Вложение ${index + 1}`} src={asset.url} /><span>{asset.width} x {asset.height}</span>
                      </div>
                    ))}
                  </div>
                ) : <div className="section-empty">Нет изображений</div>}
              </section>

              <section className="editor-section">
                <div className="section-title-row">
                  <span className="field-label"><Link2 size={14} /> Связи</span><span className="count-badge">{linkedNodeIds.length}</span>
                </div>
                <div className="link-list">
                  {linkedNodeIds.map((id) => (
                    <div className="link-item" key={id}>
                      <span><span className="link-dot" />{treeTitleMap.get(id) || id.slice(0, 8)}</span>
                      <button aria-label="Удалить связь" className="bare-icon-button" onClick={() => setLinkedNodeIds((current) => current.filter((item) => item !== id))} type="button"><Unlink size={14} /></button>
                    </div>
                  ))}
                </div>
                {availableLinkTargets.length > 0 && (
                  <select
                    aria-label="Добавить связь" className="select-input"
                    onChange={(event) => {
                      if (event.target.value) setLinkedNodeIds((current) => [...current, event.target.value])
                      event.target.value = ''
                    }} value=""
                  >
                    <option value="">Добавить связь...</option>
                    {availableLinkTargets.map((item) => <option key={item.id} value={item.id}>{item.title}</option>)}
                  </select>
                )}
              </section>

              <section className="editor-section layer-section">
                <div>
                  <span className="field-label"><Layers3 size={14} /> Вложенный слой</span>
                  <p>{selectedNode.childLayerId ? 'Внутренняя ветка доступна' : 'Внутренняя ветка не создана'}</p>
                </div>
                <button className="secondary-button full-width" onClick={() => void handleCreateOrOpenChild()} type="button"><Layers3 size={16} /> {selectedNode.childLayerId ? 'Открыть слой' : 'Создать слой'}</button>
              </section>
              <div className="editor-actions">
                <button className="primary-button grow" disabled={savingNode || !editorTitle.trim()} onClick={() => void handleSaveNode()} type="button">{savingNode ? <Spinner /> : <Save size={16} />} Сохранить</button>
                <button aria-label="Удалить узел" className="danger-icon-button" onClick={() => setShowDeleteDialog(true)} title="Удалить узел" type="button"><Trash2 size={17} /></button>
              </div>
            </div>
          )}
        </aside>
      </div>

      {(leftOpen || rightOpen) && <div className="mobile-scrim" onClick={() => { setLeftOpen(false); setRightOpen(false) }} />}

      {showCreateNode && (
        <Modal onClose={() => setShowCreateNode(false)} title="Новый узел">
          <div className="modal-body form-stack">
            <div>
              <label className="field-label" htmlFor="new-node-title">Название</label>
              <input autoFocus className="text-input" id="new-node-title" onChange={(event) => setNewNodeTitle(event.target.value)} placeholder="Например, Ключевое событие" value={newNodeTitle} />
            </div>
            <div>
              <label className="field-label" htmlFor="new-node-text">Содержание</label>
              <textarea className="content-textarea" id="new-node-text" onChange={(event) => setNewNodeText(event.target.value)} placeholder="Что происходит в этой точке narrative..." rows={6} value={newNodeText} />
            </div>
          </div>
          <footer className="modal-footer">
            <button className="secondary-button" onClick={() => setShowCreateNode(false)} type="button">Отмена</button>
            <button className="primary-button" disabled={creatingNode || !newNodeTitle.trim()} onClick={() => void handleCreateNode()} type="button">{creatingNode ? <Spinner /> : <Plus size={16} />} Создать</button>
          </footer>
        </Modal>
      )}

      {showProjectDialog && <ProjectModal {...{ projectDraft, setProjectDraft, projectSwitching, handleProjectSwitch }} onClose={() => setShowProjectDialog(false)} />}

      {showDeleteDialog && selectedNode && (
        <Modal onClose={() => setShowDeleteDialog(false)} title="Удалить узел?" width="small">
          <div className="modal-body delete-copy">
            <div className="delete-warning"><Trash2 size={20} /></div>
            <p><strong>{selectedNode.title}</strong> и все его вложенные слои будут удалены без возможности восстановления.</p>
          </div>
          <footer className="modal-footer">
            <button className="secondary-button" onClick={() => setShowDeleteDialog(false)} type="button">Отмена</button>
            <button className="danger-button" disabled={deletingNode} onClick={() => void handleDeleteNode()} type="button">{deletingNode ? <Spinner /> : <Trash2 size={16} />} Удалить</button>
          </footer>
        </Modal>
      )}

      <ToastStack toasts={toasts} />
    </main>
  )
}

function ProjectModal({ projectDraft, setProjectDraft, projectSwitching, handleProjectSwitch, onClose }: {
  projectDraft: string
  setProjectDraft: (value: string) => void
  projectSwitching: boolean
  handleProjectSwitch: () => Promise<void>
  onClose: () => void
}) {
  return (
    <Modal onClose={onClose} title="Открыть проект" width="small">
      <div className="modal-body">
        <label className="field-label" htmlFor="project-id">ID проекта</label>
        <input
          autoFocus className="text-input mono-input" id="project-id"
          onChange={(event) => setProjectDraft(event.target.value)}
          onKeyDown={(event) => { if (event.key === 'Enter') void handleProjectSwitch() }}
          value={projectDraft}
        />
      </div>
      <footer className="modal-footer">
        <button className="secondary-button" onClick={onClose} type="button">Отмена</button>
        <button className="primary-button" disabled={projectSwitching} onClick={() => void handleProjectSwitch()} type="button">
          {projectSwitching ? <Spinner /> : <ArrowRight size={16} />} Открыть
        </button>
      </footer>
    </Modal>
  )
}

function ToastStack({ toasts }: { toasts: Toast[] }) {
  return (
    <div className="toast-stack">
      {toasts.map((toast) => (
        <div className={`toast toast-${toast.kind}`} key={toast.id}>
          {toast.kind === 'success' ? <Check size={16} /> : toast.kind === 'error' ? <CircleAlert size={16} /> : null}
          {toast.message}
        </div>
      ))}
    </div>
  )
}

export default App
