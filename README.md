# HeartCode 心码 - AI 应用生成平台

> 基于 AI 的前端代码生成与部署平台，通过自然语言对话生成完整 Web 应用，支持在线预览、可视化编辑、一键部署和源码下载。

## 项目简介

HeartCode 心码是一个 AI 驱动的应用生成平台（对标 美团-NoCode、百度-秒哒），让用户无需编写代码，只需用自然语言描述需求，AI 即可生成完整可运行的 Web 应用。平台支持三种代码生成模式，从简单的单文件 HTML 到完整的 Vue 工程，满足不同复杂度的应用需求。

### 核心能力

- **三种生成模式**：原生 HTML / 原生多文件 / Vue 工程（Agent 工具调用）
- **流式输出**：基于 SSE 协议，AI 生成内容实时推送，打字机效果
- **可视化编辑**：点击预览页面元素，AI 精准定位修改目标（所见即所指）
- **一键部署**：生成 deployKey，支持应用在线访问
- **源码下载**：智能过滤 node_modules，打包下载完整源码
- **多轮对话**：AI 具备上下文记忆，支持增量修改
- **自动截图**：Selenium 无头浏览器自动截图作为应用封面

<img width="2690" height="1169" alt="image" src="https://github.com/user-attachments/assets/c4745b6d-6097-4add-bb02-0214d1b1ec8e" />
<img width="976" height="1418" alt="image" src="https://github.com/user-attachments/assets/0babb2fa-d1b3-44be-b533-4f6e99e4ec3a" />
<img width="2762" height="1447" alt="image" src="https://github.com/user-attachments/assets/a7451d75-97cd-449e-952a-9940ad909d6b" />



## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | 虚拟线程支持 |
| Spring Boot | 3.5.13 | 核心框架 |
| LangChain4j | 1.1.0 | AI 大模型框架 |
| MyBatis-Flex | 1.11.0 | ORM 框架 |
| MySQL | 8.0 | 关系型数据库 |
| Redis | 7.0 | 缓存 / Session / 限流 |
| Redisson | - | 分布式限流器 |
| Caffeine | - | 本地缓存 |
| Selenium | 4.x | 无头浏览器截图 |
| 腾讯云 COS | - | 对象存储 |
| Knife4j | - | 接口文档 |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue 3 | 3.5.x | 组合式 API |
| TypeScript | 5.8.x | 类型安全 |
| Vite | 7.0.x | 构建工具 |
| Pinia | 3.0.x | 状态管理 |
| Vue Router | 4.5.x | 路由管理 |
| Ant Design Vue | 4.2.x | UI 组件库 |
| marked + DOMPurify | - | Markdown 渲染 + XSS 防护 |

### AI 大模型

| 模型 | 说明 |
|------|------|
| 智谱 GLM-5.2 | 通过 OpenAI 兼容接口调用，支持流式输出和工具调用 |

## 系统架构

```
┌─────────────────────────────────────────────────────┐
│              前端 (Vue 3 + Vite + TS)                │
│  首页 / 对话页 / 管理后台 / 用户中心                  │
└──────────────────────┬──────────────────────────────┘
                       │ HTTP / SSE
┌──────────────────────▼──────────────────────────────┐
│            后端 (Spring Boot 3 + Java 21)            │
│  ┌────────────────────────────────────────────────┐ │
│  │  Controller 层（应用/对话/用户/资源）            │ │
│  ├────────────────────────────────────────────────┤ │
│  │  Service 层（业务编排）                          │ │
│  ├────────────────────────────────────────────────┤ │
│  │  AI 核心层                                      │ │
│  │  Facade(门面) → Factory(工厂) → AI Service     │ │
│  │  Tools(Agent工具) / Parser / Saver / Handler   │ │
│  ├────────────────────────────────────────────────┤ │
│  │  基础设施层（权限AOP / 限流AOP / 多级缓存）       │ │
│  └────────────────────────────────────────────────┘ │
└──────┬──────────┬──────────┬──────────┬──────────────┘
       │          │          │          │
   MySQL       Redis      腾讯云COS    AI大模型
  (持久化)    (缓存/记忆)  (文件存储)  (GLM-5.2)
```

### AI 代码生成核心架构

```
用户消息
    │
    ▼
┌─────────────────┐     ┌─────────────────────────────────┐
│ AiCodeGenerator │────▶│ AiCodeGeneratorServiceFactory    │
│    Facade       │     │ (Caffeine 缓存 + 记忆回灌)        │
│   (门面层)      │     └──────────────┬──────────────────┘
└─────────────────┘                    │
                    ┌─────────────────┼─────────────────┐
                    │                 │                  │
                    ▼                 ▼                  ▼
              HTML 模式         多文件模式          Vue 工程模式
              (文本架构)        (文本架构)         (Agent 架构)
                    │                 │                  │
                    ▼                 ▼                  ▼
              Parser+Saver      Parser+Saver       Tools(5个工具)
              (解析+保存)        (解析+保存)        (AI 直写文件)
```

## 核心特性

### 1. 三种代码生成模式

| 模式 | 架构 | 适用场景 | 构建方式 |
|------|------|---------|---------|
| 原生 HTML | 文本生成 | 简单页面、工具、计算器 | 无需构建 |
| 原生多文件 | 文本生成 | 中等应用、CSS/JS 分离 | 无需构建 |
| Vue 工程 | Agent 工具调用 | 复杂应用、完整项目 | npm 构建 |

### 2. AI Agent 工具系统

Vue 工程模式下，AI 以智能体身份自主调用 5 个文件操作工具：

- `FileWriteTool` - 写入文件
- `FileReadTool` - 读取文件
- `FileModifyTool` - 精确替换修改
- `FileDeleteTool` - 删除文件（含重要文件保护）
- `FileDirReadTool` - 读取目录结构

### 3. 可视化编辑器

通过 iframe postMessage 双向通信实现"所见即所指"：
- 点击预览元素 → 高亮选中 → 捕获元素上下文
- 静默附加到用户消息 → AI 精准定位修改目标

### 4. 对话记忆机制

采用 MySQL + Redis 双存储架构：
- Redis 维护滑动窗口记忆（20 条）
- MySQL 持久化全部对话历史
- 实例缓存过期后自动从数据库回灌记忆

### 5. 安全防护体系

- **Prompt 注入防护**：InputGuardrail 输入护栏（敏感词 + 注入模式匹配）
- **输出安全**：OutputGuardrail + DOMPurify XSS 消毒
- **分布式限流**：Redisson 令牌桶（用户级 / IP 级 / 接口级）
- **路径穿越防护**：静态资源访问目录校验
- **三层权限**：会话层 + 角色层 + 资源层

## 项目结构

```
HeartCode/
├── src/main/java/com/chp/heartcode/
│   ├── ai/                          # AI 核心层
│   │   ├── guardrail/               # 安全护栏（输入/输出）
│   │   ├── model/                   # AI 消息模型
│   │   ├── tools/                   # Agent 工具（文件操作）
│   │   └── AiCodeGeneratorService.java  # AI 服务接口
│   ├── core/                        # 代码生成核心
│   │   ├── builder/                 # 项目构建器
│   │   ├── handler/                 # 流处理器
│   │   ├── parser/                  # 代码解析器
│   │   ├── saver/                   # 文件保存器
│   │   └── AiCodeGeneratorFacade.java   # 门面层
│   ├── config/                      # 配置类
│   │   ├── AiCodeGeneratorServiceFactory.java  # AI 服务工厂
│   │   └── ...
│   ├── controller/                  # 控制器
│   ├── service/                     # 业务逻辑
│   ├── aop/                         # 权限切面
│   ├── ratelimiter/                 # 限流组件
│   └── ...
├── src/main/resources/
│   ├── prompt/                      # 系统提示词（4 种模式）
│   ├── application.yml              # 应用配置
│   └── ...
├── heartcode-frontend/              # 前端项目
│   └── src/
│       ├── pages/                   # 页面组件
│       ├── components/              # 公共组件
│       ├── stores/                  # Pinia 状态管理
│       ├── api/                     # API 请求层
│       └── utils/                   # 工具类（含可视化编辑器）
├── sql/
│   └── create_table.sql             # 数据库建表脚本
└── pom.xml                          # Maven 依赖配置
```
<img width="1012" height="1344" alt="image" src="https://github.com/user-attachments/assets/97ef4bf7-bdb6-4da3-a297-8f23fadf3cfa" />
<img width="1004" height="1401" alt="image" src="https://github.com/user-attachments/assets/860fa31d-8668-437b-90dd-a737ce70fbba" />



## 快速开始

### 环境要求

- **JDK** 21+
- **Node.js** 18+ LTS
- **MySQL** 8.0+
- **Redis** 7.0+
- **Chrome** + **ChromeDriver**（用于截图功能）

### 1. 克隆项目

```bash
git clone https://github.com/你的用户名/HeartCode.git
cd HeartCode
```

### 2. 初始化数据库

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS heartcode DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 执行建表脚本
mysql -u root -p heartcode < sql/create_table.sql
```

### 3. 配置后端

修改 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/heartcode
    username: root
    password: <你的数据库密码>
  data:
    redis:
      host: localhost
      port: 6379
      password: <你的Redis密码，没有则留空>

# AI 大模型配置（智谱 GLM）
langchain4j:
  open-ai:
    chat-model:
      api-key: <你的智谱API Key>
      base-url: https://open.bigmodel.cn/api/coding/paas/v4
      model-name: glm-5.2
    streaming-chat-model:
      api-key: <你的智谱API Key>
      base-url: https://open.bigmodel.cn/api/coding/paas/v4
      model-name: glm-5.2

# 腾讯云 COS 配置（用于截图存储）
cos:
  client:
    host: <你的COS域名>
    secretId: <你的SecretId>
    secretKey: <你的SecretKey>
    region: <你的COS区域>
    bucket: <你的Bucket名称>
```

> 智谱 API Key 获取：访问 [智谱AI开放平台](https://open.bigmodel.cn/) 注册并创建 API Key

### 4. 启动后端

```bash
# Windows
mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

后端启动后访问 `http://localhost:8123/api/doc.html` 查看接口文档。

<img width="2696" height="1542" alt="image" src="https://github.com/user-attachments/assets/07ef5dc0-8c06-485c-8c0c-a61689f282d3" />
<img width="2839" height="1446" alt="image" src="https://github.com/user-attachments/assets/01dfe5e2-04a5-4687-9126-487fda323c4c" />



### 5. 启动前端

```bash
cd heartcode-frontend
npm install
npm run dev
```

前端启动后访问 `http://localhost:5173` 即可使用。

<img width="1570" height="718" alt="image" src="https://github.com/user-attachments/assets/35d7f4cf-7798-4f52-935a-8acaafd9dc2f" />


## 默认账号

数据库初始化后，可通过注册页面创建账号，或手动插入管理员账号：

```sql
-- 插入管理员账号（密码需经过 MD5 + 盐值加密）
INSERT INTO `user` (`id`, `userAccount`, `userPassword`, `userName`, `userRole`)
VALUES (1, 'admin', '<加密后的密码>', '管理员', 'admin');
```

<img width="2815" height="1366" alt="image" src="https://github.com/user-attachments/assets/2ca2ef6b-24de-4f7a-9b8a-b98d243e57e0" />


## 功能演示

### 使用流程

1. **注册登录** - 创建账号并登录系统
2. **描述需求** - 在首页输入框描述你想要的应用，选择生成模式
3. **AI 生成** - AI 流式生成代码，实时预览效果
4. **多轮修改** - 继续对话修改细节，或使用可视化编辑器点击修改
5. **一键部署** - 点击部署，获取在线访问链接
6. **下载源码** - 下载完整项目源码到本地

### 支持的应用类型

- 工具类：BMI 计算器、单位换算、密码生成器
- 效率类：待办事项、番茄钟、笔记应用
- 展示类：个人主页、产品介绍页
- 游戏类：贪吃蛇、2048、井字棋
- 复杂应用：任务管理系统、博客系统（Vue 工程模式）

## 设计模式运用

| 设计模式 | 应用位置 |
|---------|---------|
| 门面模式 | `AiCodeGeneratorFacade` 统一编排生成流程 |
| 工厂模式 | `AiCodeGeneratorServiceFactory` 创建隔离的 AI 实例 |
| 策略模式 | `CodeParser` / `CodeFileSaverTemplate` 差异化处理 |
| 模板方法 | `CodeFileSaverTemplate.saveCode()` 定义保存骨架 |
| 执行器模式 | Parser/Saver/StreamHandler Executor 分发解耦 |

## 项目亮点

1. **多模式 AI 代码生成** - 从简单 HTML 到完整 Vue 工程的渐进式生成
2. **AI Agent 工具系统** - Vue 模式下 AI 自主调用工具，实现智能体编程
3. **可视化编辑器** - postMessage 实现"所见即所指"，降低使用门槛
4. **对话记忆双存储** - 数据库 + Redis 回灌设计，上下文不丢失
5. **应用级全链路隔离** - appId 贯穿记忆、目录、实例，并发安全
6. **流式响应体验** - SSE + 打字机效果 + 实时 Markdown 渲染
<img width="2747" height="1437" alt="image" src="https://github.com/user-attachments/assets/98a88371-e3f3-47ee-a8ea-83f2b4abda4b" />
<img width="1931" height="1421" alt="image" src="https://github.com/user-attachments/assets/1a83d949-3ee8-4e3f-86dc-f1e600ff5062" />
<img width="2687" height="1188" alt="image" src="https://github.com/user-attachments/assets/f3ac9028-0208-4b2b-94d0-aa5b35b1beba" />
<img width="1260" height="899" alt="image" src="https://github.com/user-attachments/assets/6f58c809-3674-42a9-a9f8-d5387b88b3ff" />
<img width="2057" height="1098" alt="image" src="https://github.com/user-attachments/assets/c11101da-4e5b-4da9-bac6-675a12b84e56" />
<img width="2525" height="1430" alt="image" src="https://github.com/user-attachments/assets/c05131ff-891e-4a27-a306-3254388a01ac" />
<img width="2824" height="1458" alt="image" src="https://github.com/user-attachments/assets/548aecaa-ee01-4fbb-b92b-dc399de45280" />
<img width="2834" height="1452" alt="image" src="https://github.com/user-attachments/assets/2fd4eed3-fc2f-4b3f-966c-062b07e7671e" />
<img width="2828" height="1444" alt="image" src="https://github.com/user-attachments/assets/6b5c1438-45d7-4eb5-b6a7-45103d78c903" />
<img width="2817" height="1443" alt="image" src="https://github.com/user-attachments/assets/9b39ce78-a0d9-4105-9012-84c366768a0e" />
<img width="2817" height="1453" alt="image" src="https://github.com/user-attachments/assets/1f63e014-4679-4084-96d5-af8126a1a966" />


## 所有权声明

本项目（HeartCode 心码 - AI 应用生成平台）由 **chp** 独立设计并开发，相关知识产权归作者所有。

- 本项目仅用于 **学术交流** 目的，不得用于任何商业用途。
- 未经作者书面许可，禁止将本项目代码、设计文档、论文内容等用于商业出版、售卖或二次分发。
- 引用本项目代码或设计思想时，请注明出处并保留原作者信息。
- 作者保留对本项目的最终解释权。








