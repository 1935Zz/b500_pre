# b500_pre

## Repo简介

这是一个基于Java和LWJGL的六边形地图可视化项目，用于生成和渲染包含省份、城市、道路、河流和森林的地图。

**项目名称**：Hex Map Visualizer - 六边形地图可视化器

**主要功能**：
- 生成六边形网格地图
- 随机生成森林、河流等地形
- 支持动态添加城市并通过道路连接
- 使用OpenGL进行实时渲染
- 支持多种可视化模式

**技术栈**：
- Java
- LWJGL (Lightweight Java Game Library) - OpenGL绑定
- JOML (Java OpenGL Math Library) - 数学库
- Maven - 依赖管理

**项目结构**：
- `src/Main.java` - 主程序入口，处理窗口、输入和渲染循环
- `src/map/ProvinceMap.java` - 地图数据结构和逻辑，包含省份、邻接关系、路径查找等
- `src/map/Province.java` - 省份类，包含地形类型、颜色、道路等信息
- `src/map/HexMapGenerator.java` - 六边形地图生成器
- `src/render/MapRenderer.java` - 地图渲染器，负责OpenGL渲染
- `src/render/SpriteRenderer.java` - 精灵渲染器
- `src/render/ResourceManager.java` - 资源管理器
- `src/render/Shader.java` - Shader管理
- `pom.xml` - Maven配置文件
- `README.md` - 项目说明文档
- `Dockerfile` 和构建脚本 - Docker容器化支持

**核心算法**：
- 六边形网格生成
- Dijkstra最短路径算法（考虑地形成本和道路）
- 随机河流生成（带分叉）
- 随机森林生成

**当前状态**：
- 基本地图生成和渲染功能完整
- 支持动态添加城市和道路
- 需要添加三种可视化模式的切换功能

## 题目Prompt

I want some more ways to visualise the map. Modify the code so that there are three modes which I can switch between using 1, 2, and 3. 1 for the current visuals. Mode 2 should visualise how far each province is from a city. Then a mode which colours provinces according to their "rating", where:
- adjacency to at least one city gives -10 rating, or -5 if one province separated
- adjacency to at least one river gives +3 rating
- being on a road gives +3
- -5 if in forest, otherwise +1 if adjacent to any forest.
All modes should still show cities, roads, and rivers.

## PR链接

待创建
