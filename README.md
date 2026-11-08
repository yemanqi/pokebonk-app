# PokeBonk 安卓客户端

把 <https://pokebonk.com/> 包成一个 Android App，内核用 **Firefox 的 GeckoView**。

## 为什么不是 WebView

`pokebonk.com` 和 `api.pokebonk.com` 都有 IPv6 解析记录。部分网络（尤其国内移动网络）会优先走 IPv6，而到 Cloudflare 那段经常断，表现就是卡住或加载不出来。

GeckoView 自带网络栈（Necko），可以对指定域名强制走 IPv4：

- `network.dns.disableIPv6 = true`
- `network.dns.ipv4OnlyDomains = pokebonk.com, api.pokebonk.com`
- `network.http.fast-fallback-to-IPv4 = true`

## 做了什么

- 全屏，无地址栏
- 返回键先退网页、再退 App
- 断网显示「连不上 + 重试」，不会卡白屏
- 加载中显示进度提示
- 输入框获得焦点时主动呼出软键盘（GeckoView 不会自己弹）
- 游戏内容、账号、存档全部直连官方服务器，不经过本 App

## 下载

从 [Releases](https://github.com/yemanqi/pokebonk-app/releases) 下载最新 APK。

- 只支持 **arm64** 手机
- 约 165MB（整个 Firefox 内核）
- debug 签名，可正常安装使用，不能上架应用商店
- 部分手机的「安全键盘」会拦掉网页输入框的键盘，遇到打字没反应请关掉它

## 自己构建

```bash
# 推送到 main 分支会自动触发 GitHub Actions 打包
git push origin main
```

产物在 Actions → 最近一次 Build APK → Artifacts。

## 免责

个人非商业的玩家封装。Pokémon 及相关名称是 Nintendo / Game Freak / Creatures 的商标。游戏由原作者制作，本仓库只做了安卓端封装。
